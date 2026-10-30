package com.sakurastore.backend.infrastructure.config;

import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.application.usecase.*;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

  @Bean
  public CreateUserUseCase createUserUseCase(
      UserRepositoryPort userRepositoryPort,
      RoleRepositoryPort roleRepositoryPort,
      PasswordEncoderPort passwordEncoderPort) {
    return new CreateUserUseCase(userRepositoryPort, roleRepositoryPort, passwordEncoderPort);
  }

  @Bean
  public UpdateUserUseCase updateUserUseCase(
      UserRepositoryPort userRepositoryPort, RoleRepositoryPort roleRepositoryPort) {
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
  public DeleteUserUseCase deleteUserUseCase(
      UserRepositoryPort userRepositoryPort,
      EmailVerificationRepositoryPort emailVerificationRepositoryPort) {
    return new DeleteUserUseCase(userRepositoryPort, emailVerificationRepositoryPort);
  }
}
