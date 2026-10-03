package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

public class DeleteUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;

    public DeleteUserUseCase(UserRepositoryPort userRepositoryPort,
                             EmailVerificationRepositoryPort emailVerificationRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.emailVerificationRepositoryPort = emailVerificationRepositoryPort;
    }

    public ApiResponseDto execute(Long id) {
        if (!userRepositoryPort.existsById(id)) {
            throw new DomainException("El usuario con ID " + id + " no existe.");
        }

        emailVerificationRepositoryPort.invalidateAllPendingByUserId(id);
        userRepositoryPort.deleteById(id);

        return new ApiResponseDto("Usuario eliminado exitosamente.");
    }
}
