package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.UpdateUserCommand;
import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

import com.sakurastore.backend.domain.model.RoleEnum;

public class UpdateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;

    public UpdateUserUseCase(UserRepositoryPort userRepositoryPort, RoleRepositoryPort roleRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.roleRepositoryPort = roleRepositoryPort;
    }

    public UserResponseDto execute(Long id, UpdateUserCommand command) {
        return execute(id, command, null);
    }

    public UserResponseDto execute(Long id, UpdateUserCommand command, String requestorRole) {
        User existingUser = userRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("El usuario con ID " + id + " no fue encontrado."));

        if (!existingUser.getEmail().equalsIgnoreCase(command.getEmail().trim())) {
            if (userRepositoryPort.existsByEmail(command.getEmail())) {
                throw new DomainException("El correo '" + command.getEmail() + "' ya pertenece a otro usuario.");
            }
        }

        Role newRole = roleRepositoryPort.findById(command.getRoleId())
                .orElseThrow(() -> new DomainException("El rol con ID " + command.getRoleId() + " no fue encontrado."));

        if (!existingUser.getRole().getId().equals(newRole.getId())) {
            if (requestorRole == null || !RoleEnum.ROLE_ADMIN.name().equalsIgnoreCase(requestorRole.trim())) {
                throw new DomainException("Solo un Administrador tiene permisos para cambiar el rol de los usuarios.");
            }
        }

        existingUser.updateDetails(command.getFullName(), command.getEmail(), newRole);
        User updatedUser = userRepositoryPort.save(existingUser);
        return UserResponseDto.fromDomain(updatedUser);
    }
}
