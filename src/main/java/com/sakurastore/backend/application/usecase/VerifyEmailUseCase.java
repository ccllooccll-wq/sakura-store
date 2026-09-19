package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.AuthResponseDto;
import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.application.dto.VerifyEmailCommand;
import com.sakurastore.backend.application.port.JwtTokenProviderPort;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.EmailVerification;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

public class VerifyEmailUseCase {

    private static final int MAX_ATTEMPTS = 5;
    private final UserRepositoryPort userRepositoryPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final JwtTokenProviderPort jwtTokenProviderPort;

    public VerifyEmailUseCase(UserRepositoryPort userRepositoryPort,
                             EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                             PasswordEncoderPort passwordEncoderPort,
                             JwtTokenProviderPort jwtTokenProviderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.emailVerificationRepositoryPort = emailVerificationRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.jwtTokenProviderPort = jwtTokenProviderPort;
    }

    public AuthResponseDto execute(VerifyEmailCommand command) {
        String cleanEmail = command.getEmail().trim().toLowerCase();

        User user = userRepositoryPort.findByEmail(cleanEmail)
                .orElseThrow(() -> new DomainException("No se encontró ninguna cuenta registrada con ese correo electrónico."));

        if (!user.isActive()) {
            throw new DomainException("El usuario se encuentra desactivado. Por favor contacte al Administrador.");
        }

        EmailVerification verification = emailVerificationRepositoryPort.findLatestByUserId(user.getId())
                .orElseThrow(() -> new DomainException("No existe un código de verificación activo para este correo. Solicita uno nuevo."));

        if (verification.isUsed()) {
            throw new DomainException("Este código de verificación ya ha sido utilizado. Solicita uno nuevo.");
        }

        if (verification.hasExceededAttempts(MAX_ATTEMPTS)) {
            throw new DomainException("Has superado el límite de intentos permitidos (5). Solicita un nuevo código de verificación.");
        }

        verification.incrementAttempts();
        emailVerificationRepositoryPort.save(verification);

        if (verification.isExpired()) {
            throw new DomainException("El código de verificación ha expirado. Por favor, solicita uno nuevo.");
        }

        if (!passwordEncoderPort.matches(command.getCodigo(), verification.getCodeHash())) {
            throw new DomainException("El código introducido es incorrecto. Verifica los 6 dígitos ingresados.");
        }

        verification.markAsUsed();
        emailVerificationRepositoryPort.save(verification);

        user.markEmailAsVerified();
        User savedUser = userRepositoryPort.save(user);

        String token = jwtTokenProviderPort.generateToken(savedUser);
        AuthResponseDto response = new AuthResponseDto(token, UserResponseDto.fromDomain(savedUser));
        response.setMensaje("Código verificado exitosamente. Sesión iniciada.");
        return response;
    }
}
