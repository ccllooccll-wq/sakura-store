package com.sakurastore.backend.infrastructure.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResendEmailAdapter implements EmailSenderPort {

  private final HttpClient client = HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .build();

  private final ObjectMapper mapper;
  private final String clientId;
  private final String clientSecret;
  private final String refreshToken;
  private final String from;

  public ResendEmailAdapter(
          ObjectMapper mapper,
          @Value("${app.gmail.client-id:}") String clientId,
          @Value("${app.gmail.client-secret:}") String clientSecret,
          @Value("${app.gmail.refresh-token:}") String refreshToken,
          @Value("${app.gmail.from:}") String from
  ) {
    this.mapper = mapper;
    this.clientId = clientId.trim();
    this.clientSecret = clientSecret.trim();
    this.refreshToken = refreshToken.trim();
    this.from = from.trim();
  }

  @Override
  public void sendVerificationCode(String to, String name, String code) {
    if (clientId.isBlank()
            || clientSecret.isBlank()
            || refreshToken.isBlank()
            || from.isBlank()) {
      throw new MailDeliveryException(
              "Falta configurar el envío de correo con Gmail."
      );
    }

    validateAddress(from);
    validateAddress(to);

    try {
      String accessToken = obtainAccessToken();

      String text = "Hola " + name + ".\n\n"
              + "Tu código de Sakura Store es: " + code + "\n\n"
              + "Vence en 10 minutos. No lo compartas.\n"
              + "Si no solicitaste este código, ignora este mensaje.";

      String encodedBody = Base64.getMimeEncoder()
              .encodeToString(text.getBytes(StandardCharsets.UTF_8));

      String mime = "From: " + from + "\r\n"
              + "To: " + to.trim() + "\r\n"
              + "Subject: Sakura Store - Codigo de verificacion\r\n"
              + "MIME-Version: 1.0\r\n"
              + "Content-Type: text/plain; charset=UTF-8\r\n"
              + "Content-Transfer-Encoding: base64\r\n"
              + "\r\n"
              + encodedBody;

      String raw = Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(mime.getBytes(StandardCharsets.UTF_8));

      String json = mapper.writeValueAsString(Map.of("raw", raw));

      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(
                      "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
              ))
              .timeout(Duration.ofSeconds(20))
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();

      HttpResponse<String> response = client.send(
              request,
              HttpResponse.BodyHandlers.ofString()
      );

      int status = response.statusCode();

      if (status == 401) {
        throw new MailDeliveryException(
                "Gmail rechazó la autorización. Revisa las credenciales OAuth."
        );
      }

      if (status == 403) {
        throw new MailDeliveryException(
                "Gmail denegó el envío. Revisa Gmail API, el permiso "
                        + "gmail.send y las restricciones de la cuenta."
        );
      }

      if (status == 429) {
        throw new MailDeliveryException(
                "Se alcanzó un límite de Gmail. Inténtalo más tarde."
        );
      }

      if (status < 200 || status >= 300) {
        throw new MailDeliveryException(
                "Gmail rechazó el envío. Estado HTTP: " + status
        );
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MailDeliveryException("El envío fue interrumpido.");
    } catch (MailDeliveryException e) {
      throw e;
    } catch (Exception e) {
      throw new MailDeliveryException(
              "No se pudo completar el envío. Revisa tu conexión "
                      + "y solicita otro código."
      );
    }
  }

  private String obtainAccessToken() throws Exception {
    String body = "client_id=" + encode(clientId)
            + "&client_secret=" + encode(clientSecret)
            + "&refresh_token=" + encode(refreshToken)
            + "&grant_type=refresh_token";

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .timeout(Duration.ofSeconds(15))
            .header(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
            )
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
    );

    JsonNode result = mapper.readTree(response.body());

    if (response.statusCode() != 200) {
      String error = result.path("error").asText();

      if ("invalid_grant".equals(error)) {
        throw new MailDeliveryException(
                "La autorización de Gmail venció o fue revocada. "
                        + "Genera un nuevo refresh token."
        );
      }

      throw new MailDeliveryException(
              "No se pudo autorizar Gmail. Revisa Client ID, "
                      + "Client secret y Refresh token."
      );
    }

    String token = result.path("access_token").asText();

    if (token.isBlank()) {
      throw new MailDeliveryException(
              "Google no devolvió un token de acceso."
      );
    }

    return token;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static void validateAddress(String address) {
    if (address == null
            || !address.trim().matches(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+$"
    )) {
      throw new MailDeliveryException(
              "La dirección de correo no tiene un formato válido."
      );
    }
  }

  public static class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message) {
      super(message);
    }
  }
}