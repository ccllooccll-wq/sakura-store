package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.application.dto.RegisterUserCommand;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.EmailVerification;
import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.RoleEnum;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

import java.security.SecureRandom;

public class RegisterUserUseCase {

    private static final int EXPIRATION_MINUTES = 10;
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegisterUserUseCase(UserRepositoryPort userRepositoryPort,
                               RoleRepositoryPort roleRepositoryPort,
                               EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                               EmailSenderPort emailSenderPort,
                               PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.roleRepositoryPort = roleRepositoryPort;
        this.emailVerificationRepositoryPort = emailVerificationRepositoryPort;
        this.emailSenderPort = emailSenderPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public ApiResponseDto execute(RegisterUserCommand command) {
        String cleanEmail = command.getEmail().trim().toLowerCase();
        String cleanUsername = command.getUsername().trim();

        if (userRepositoryPort.existsByUsername(cleanUsername)) {
            throw new DomainException("El nombre de usuario '" + cleanUsername + "' ya se encuentra registrado.");
        }
        if (userRepositoryPort.existsByEmail(cleanEmail)) {
            throw new DomainException("El correo electrónico '" + cleanEmail + "' ya se encuentra registrado.");
        }

        Role userRole = roleRepositoryPort.findByName(RoleEnum.ROLE_VENDEDOR)
                .orElseGet(() -> roleRepositoryPort.findAll().stream().findFirst()
                        .orElseThrow(() -> new DomainException("No existe un rol disponible para el nuevo usuario.")));

        String encodedPassword = passwordEncoderPort.encode(command.getPassword());
        User newUser = User.createNewUser(
                cleanUsername,
                command.getNombre(),
                cleanEmail,
                encodedPassword,
                userRole
        );

        User savedUser = userRepositoryPort.save(newUser);

        emailVerificationRepositoryPort.invalidateAllPendingByUserId(savedUser.getId());

        String rawCode = String.format("%06d", secureRandom.nextInt(1000000));
        String codeHash = passwordEncoderPort.encode(rawCode);

        EmailVerification verification = EmailVerification.create(savedUser.getId(), codeHash, EXPIRATION_MINUTES);
        emailVerificationRepositoryPort.save(verification);

        emailSenderPort.sendVerificationCode(savedUser.getEmail(), savedUser.getFullName(), rawCode);

        return new ApiResponseDto("Usuario registrado. Revisa tu correo para verificar tu cuenta.");
    }
}
