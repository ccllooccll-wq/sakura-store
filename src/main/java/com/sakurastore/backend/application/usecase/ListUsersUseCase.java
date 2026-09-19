package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.domain.port.UserRepositoryPort;

import java.util.List;
import java.util.stream.Collectors;

public class ListUsersUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public ListUsersUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public List<UserResponseDto> execute() {
        return userRepositoryPort.findAll().stream()
                .map(UserResponseDto::fromDomain)
                .collect(Collectors.toList());
    }

    public UserResponseDto findById(Long id) {
        return userRepositoryPort.findById(id)
                .map(UserResponseDto::fromDomain)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID " + id));
    }
}
