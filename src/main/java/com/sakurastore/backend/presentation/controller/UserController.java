package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.application.dto.CreateUserCommand;
import com.sakurastore.backend.application.dto.UpdateUserCommand;
import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.application.usecase.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final ToggleUserStatusUseCase toggleUserStatusUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          ListUsersUseCase listUsersUseCase,
                          ToggleUserStatusUseCase toggleUserStatusUseCase,
                          DeleteUserUseCase deleteUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.toggleUserStatusUseCase = toggleUserStatusUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(listUsersUseCase.execute());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(listUsersUseCase.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@RequestBody CreateUserCommand command) {
        UserResponseDto created = createUserUseCase.execute(command);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserCommand command,
            @RequestHeader(value = "X-User-Role", required = false) String requestorRole) {
        UserResponseDto updated = updateUserUseCase.execute(id, command, requestorRole);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        UserResponseDto updated = toggleUserStatusUseCase.execute(id, active);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteUser(@PathVariable Long id) {
        ApiResponseDto response = deleteUserUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}
