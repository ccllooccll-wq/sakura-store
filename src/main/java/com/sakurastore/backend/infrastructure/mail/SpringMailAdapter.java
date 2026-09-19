package com.sakurastore.backend.infrastructure.mail;

import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class SpringMailAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SpringMailAdapter.class);

    @Value("${spring.mail.username:}")
    private String customUsername;

    @Value("${spring.mail.password:}")
    private String customPassword;

    private record MailAccount(String email, String password) {}

    private final List<MailAccount> defaultAccounts = List.of(
            new MailAccount("danqueti123456789@gmail.com", "kztmmpqljhdsgeew"),
            new MailAccount("barbuchi32@gmail.com", "mwsgdoxlypzkoztq"),
            new MailAccount("mamanibaltazarronnyalexander@gmail.com", "xsdinyacncmhereg")
    );

    @Override
    public void sendVerificationCode(String toEmail, String recipientName, String code) {
        List<MailAccount> accountsToTry = new ArrayList<>();

        if (customUsername != null && !customUsername.isBlank() && customPassword != null && !customPassword.isBlank()) {
            accountsToTry.add(new MailAccount(customUsername.trim(), customPassword.trim()));
        }

        for (MailAccount acc : defaultAccounts) {
            if (accountsToTry.stream().noneMatch(a -> a.email().equalsIgnoreCase(acc.email()))) {
                accountsToTry.add(acc);
            }
        }

        boolean sentSuccessfully = false;
        Exception lastException = null;

        for (MailAccount account : accountsToTry) {
            try {
                JavaMailSenderImpl mailSender = createMailSender(account);
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(account.email(), "Sakura Store");
                helper.setTo(toEmail);
                helper.setSubject("Sakura Store - Verificación de correo");

                String htmlContent = buildEmailTemplate(recipientName, code);
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);
                log.info("Correo de verificación enviado exitosamente a {} utilizando la cuenta emisor [{}]", toEmail, account.email());
                sentSuccessfully = true;
                break;
            } catch (Exception e) {
                log.warn("Fallo el envío de correo a {} desde la cuenta [{}]: {}. Probando siguiente cuenta disponible...",
                        toEmail, account.email(), e.getMessage());
                lastException = e;
            }
        }

        if (!sentSuccessfully) {
            log.error("Todas las cuentas de correo fallaron al enviar el OTP a {}: {}", toEmail, lastException != null ? lastException.getMessage() : "Error desconocido");
            throw new DomainException("Error al enviar el correo electrónico de verificación. Por favor, intenta de nuevo o verifica tu conexión.");
        }
    }

    private JavaMailSenderImpl createMailSender(MailAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(account.email());
        sender.setPassword(account.password());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }

    private String buildEmailTemplate(String recipientName, String code) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; margin: 0; padding: 40px 10px; color: #f8fafc; }
                .card { max-width: 500px; margin: 0 auto; background: #1e293b; border-radius: 16px; border: 1px solid rgba(255, 117, 160, 0.2); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); text-align: center; }
                .logo { font-size: 24px; font-weight: bold; color: #ff75a0; margin-bottom: 20px; display: inline-block; }
                .title { font-size: 20px; color: #ffffff; margin-bottom: 12px; }
                .text { font-size: 14px; color: #94a3b8; line-height: 1.6; margin-bottom: 24px; }
                .otp-box { background: rgba(255, 117, 160, 0.1); border: 2px dashed #ff75a0; border-radius: 12px; padding: 18px; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #ff75a0; margin: 20px 0; }
                .footer { font-size: 12px; color: #64748b; margin-top: 30px; border-top: 1px solid #334155; padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">🌸 SAKURA STORE</div>
                <div class="title">Verificación de Correo Electrónico</div>
                <div class="text">
                  Hola <strong>%s</strong>,<br>
                  Tu código de verificación para Sakura Store es:
                </div>
                <div class="otp-box">%s</div>
                <div class="text">
                  Este código expirará en <strong>10 minutos</strong>.<br>
                  Si no solicitaste este registro, puedes ignorar este correo.
                </div>
                <div class="footer">
                  Sakura Store &copy; 2026 - Todos los derechos reservados
                </div>
              </div>
            </body>
            </html>
            """.formatted(recipientName, code);
    }
}
