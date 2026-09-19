package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

public class ToggleUserStatusUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public ToggleUserStatusUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public UserResponseDto execute(Long id, boolean active) {
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Usuario no encontrado con ID " + id));

        if (active) {
            user.activar();
        } else {
            user.desactivar();
        }

        User updated = userRepositoryPort.save(user);
        return UserResponseDto.fromDomain(updated);
    }
}
