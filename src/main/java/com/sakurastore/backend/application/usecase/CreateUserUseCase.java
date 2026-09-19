package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.CreateUserCommand;
import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

public class CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public CreateUserUseCase(UserRepositoryPort userRepositoryPort, RoleRepositoryPort roleRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.roleRepositoryPort = roleRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public UserResponseDto execute(CreateUserCommand command) {
        if (userRepositoryPort.existsByUsername(command.getUsername())) {
            throw new DomainException("El nombre de usuario '" + command.getUsername() + "' ya existe en Sakura Store.");
        }
        if (userRepositoryPort.existsByEmail(command.getEmail())) {
            throw new DomainException("El correo electrónico '" + command.getEmail() + "' ya se encuentra registrado.");
        }

        Role role = roleRepositoryPort.findById(command.getRoleId())
                .orElseThrow(() -> new DomainException("El rol especificado con ID " + command.getRoleId() + " no fue encontrado."));

        String encodedPassword = passwordEncoderPort.encode(command.getPassword());
        User newUser = User.createNewUser(
                command.getUsername(),
                command.getFullName(),
                command.getEmail(),
                encodedPassword,
                role
        );

        User savedUser = userRepositoryPort.save(newUser);
        return UserResponseDto.fromDomain(savedUser);
    }
}
