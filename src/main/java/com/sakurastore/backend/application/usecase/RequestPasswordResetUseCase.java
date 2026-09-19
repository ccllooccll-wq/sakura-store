package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.application.dto.RequestPasswordResetCommand;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.EmailVerification;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

import java.security.SecureRandom;

public class RequestPasswordResetUseCase {

    private static final int EXPIRATION_MINUTES = 10;
    private final UserRepositoryPort userRepositoryPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SecureRandom secureRandom = new SecureRandom();

    public RequestPasswordResetUseCase(UserRepositoryPort userRepositoryPort,
                                       EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                       EmailSenderPort emailSenderPort,
                                       PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.emailVerificationRepositoryPort = emailVerificationRepositoryPort;
        this.emailSenderPort = emailSenderPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public ApiResponseDto execute(RequestPasswordResetCommand command) {
        String cleanEmail = command.getEmail().trim().toLowerCase();

        User user = userRepositoryPort.findByEmail(cleanEmail)
                .orElseThrow(() -> new DomainException("No se encontró ninguna cuenta registrada con ese correo electrónico."));

        if (!user.isActive()) {
            throw new DomainException("El usuario se encuentra desactivado. Por favor contacte al Administrador.");
        }

        emailVerificationRepositoryPort.invalidateAllPendingByUserId(user.getId());

        String rawCode = String.format("%06d", secureRandom.nextInt(1000000));
        String codeHash = passwordEncoderPort.encode(rawCode);

        EmailVerification verification = EmailVerification.create(user.getId(), codeHash, EXPIRATION_MINUTES);
        emailVerificationRepositoryPort.save(verification);

        emailSenderPort.sendVerificationCode(user.getEmail(), user.getFullName(), rawCode);

        return new ApiResponseDto("Se ha enviado un código de verificación de 6 dígitos a tu correo electrónico (" + user.getEmail() + ").");
    }
}
