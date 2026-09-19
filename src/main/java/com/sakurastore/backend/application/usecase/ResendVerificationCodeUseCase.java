package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.application.dto.ResendCodeCommand;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.EmailVerification;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

import java.security.SecureRandom;

public class ResendVerificationCodeUseCase {

    private static final int EXPIRATION_MINUTES = 10;
    private final UserRepositoryPort userRepositoryPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SecureRandom secureRandom = new SecureRandom();

    public ResendVerificationCodeUseCase(UserRepositoryPort userRepositoryPort,
                                         EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                         EmailSenderPort emailSenderPort,
                                         PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.emailVerificationRepositoryPort = emailVerificationRepositoryPort;
        this.emailSenderPort = emailSenderPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public ApiResponseDto execute(ResendCodeCommand command) {
        String cleanEmail = command.getEmail().trim().toLowerCase();

        User user = userRepositoryPort.findByEmail(cleanEmail)
                .orElseThrow(() -> new DomainException("No se encontró ninguna cuenta asociada al correo proporcionado."));

        if (user.isEmailVerified()) {
            return new ApiResponseDto("El correo electrónico ya ha sido verificado.");
        }

        emailVerificationRepositoryPort.invalidateAllPendingByUserId(user.getId());

        String rawCode = String.format("%06d", secureRandom.nextInt(1000000));
        String codeHash = passwordEncoderPort.encode(rawCode);

        EmailVerification verification = EmailVerification.create(user.getId(), codeHash, EXPIRATION_MINUTES);
        emailVerificationRepositoryPort.save(verification);

        emailSenderPort.sendVerificationCode(user.getEmail(), user.getFullName(), rawCode);

        return new ApiResponseDto("Se ha enviado un nuevo código de verificación.");
    }
}
