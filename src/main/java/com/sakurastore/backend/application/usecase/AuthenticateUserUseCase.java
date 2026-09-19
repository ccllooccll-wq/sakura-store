package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.AuthResponseDto;
import com.sakurastore.backend.application.dto.LoginCommand;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.EmailVerification;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

import java.security.SecureRandom;

public class AuthenticateUserUseCase {
    private static final int EXPIRATION_MINUTES = 10;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthenticateUserUseCase(UserRepositoryPort userRepositoryPort,
                                   PasswordEncoderPort passwordEncoderPort,
                                   EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                   EmailSenderPort emailSenderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.emailVerificationRepositoryPort = emailVerificationRepositoryPort;
        this.emailSenderPort = emailSenderPort;
    }

    public AuthResponseDto execute(LoginCommand command) {
        String input = command.getUsername() != null ? command.getUsername().trim() : "";

        User user = userRepositoryPort.findByUsername(input)
                .or(() -> userRepositoryPort.findByEmail(input.toLowerCase()))
                .orElseThrow(() -> new DomainException("Credenciales inválidas. Usuario/correo o contraseña incorrectos."));

        if (!user.isActive()) {
            throw new DomainException("El usuario se encuentra desactivado. Por favor contacte al Administrador.");
        }

        if (!passwordEncoderPort.matches(command.getPassword(), user.getPassword())) {
            throw new DomainException("Credenciales inválidas. Usuario/correo o contraseña incorrectos.");
        }

        emailVerificationRepositoryPort.invalidateAllPendingByUserId(user.getId());

        String rawCode = String.format("%06d", secureRandom.nextInt(1000000));
        String codeHash = passwordEncoderPort.encode(rawCode);

        EmailVerification verification = EmailVerification.create(user.getId(), codeHash, EXPIRATION_MINUTES);
        emailVerificationRepositoryPort.save(verification);

        emailSenderPort.sendVerificationCode(user.getEmail(), user.getFullName(), rawCode);

        return new AuthResponseDto(true, user.getEmail(), "Se ha enviado un código de verificación de 6 dígitos a tu correo electrónico (" + user.getEmail() + ").");
    }
}
