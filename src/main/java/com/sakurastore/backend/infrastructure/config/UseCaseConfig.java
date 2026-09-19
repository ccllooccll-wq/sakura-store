package com.sakurastore.backend.infrastructure.config;

import com.sakurastore.backend.application.port.JwtTokenProviderPort;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.application.usecase.*;
import com.sakurastore.backend.domain.port.EmailSenderPort;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepositoryPort userRepositoryPort,
                                               RoleRepositoryPort roleRepositoryPort,
                                               PasswordEncoderPort passwordEncoderPort) {
        return new CreateUserUseCase(userRepositoryPort, roleRepositoryPort, passwordEncoderPort);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepositoryPort userRepositoryPort,
                                               RoleRepositoryPort roleRepositoryPort) {
        return new UpdateUserUseCase(userRepositoryPort, roleRepositoryPort);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepositoryPort userRepositoryPort) {
        return new ListUsersUseCase(userRepositoryPort);
    }

    @Bean
    public ToggleUserStatusUseCase toggleUserStatusUseCase(UserRepositoryPort userRepositoryPort) {
        return new ToggleUserStatusUseCase(userRepositoryPort);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepositoryPort userRepositoryPort,
                                               EmailVerificationRepositoryPort emailVerificationRepositoryPort) {
        return new DeleteUserUseCase(userRepositoryPort, emailVerificationRepositoryPort);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepositoryPort userRepositoryPort,
                                                   RoleRepositoryPort roleRepositoryPort,
                                                   EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                                   EmailSenderPort emailSenderPort,
                                                   PasswordEncoderPort passwordEncoderPort) {
        return new RegisterUserUseCase(userRepositoryPort, roleRepositoryPort, emailVerificationRepositoryPort, emailSenderPort, passwordEncoderPort);
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(UserRepositoryPort userRepositoryPort,
                                                 EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                                 PasswordEncoderPort passwordEncoderPort,
                                                 JwtTokenProviderPort jwtTokenProviderPort) {
        return new VerifyEmailUseCase(userRepositoryPort, emailVerificationRepositoryPort, passwordEncoderPort, jwtTokenProviderPort);
    }

    @Bean
    public ResendVerificationCodeUseCase resendVerificationCodeUseCase(UserRepositoryPort userRepositoryPort,
                                                                       EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                                                       EmailSenderPort emailSenderPort,
                                                                       PasswordEncoderPort passwordEncoderPort) {
        return new ResendVerificationCodeUseCase(userRepositoryPort, emailVerificationRepositoryPort, emailSenderPort, passwordEncoderPort);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepositoryPort userRepositoryPort,
                                                           PasswordEncoderPort passwordEncoderPort,
                                                           EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                                           EmailSenderPort emailSenderPort) {
        return new AuthenticateUserUseCase(userRepositoryPort, passwordEncoderPort, emailVerificationRepositoryPort, emailSenderPort);
    }

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(UserRepositoryPort userRepositoryPort,
                                                                   EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                                                   EmailSenderPort emailSenderPort,
                                                                   PasswordEncoderPort passwordEncoderPort) {
        return new RequestPasswordResetUseCase(userRepositoryPort, emailVerificationRepositoryPort, emailSenderPort, passwordEncoderPort);
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(UserRepositoryPort userRepositoryPort,
                                                     EmailVerificationRepositoryPort emailVerificationRepositoryPort,
                                                     PasswordEncoderPort passwordEncoderPort) {
        return new ResetPasswordUseCase(userRepositoryPort, emailVerificationRepositoryPort, passwordEncoderPort);
    }
}
