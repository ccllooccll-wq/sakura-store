package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.application.dto.CreateUserCommand;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.dto.UpdateUserCommand;
import com.sakurastore.backend.application.dto.UserResponseDto;
import com.sakurastore.backend.application.usecase.*;
import com.sakurastore.backend.infrastructure.persistence.adapter.JpaUserSearch;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final CreateUserUseCase createUserUseCase;
  private final UpdateUserUseCase updateUserUseCase;
  private final ListUsersUseCase listUsersUseCase;
  private final ToggleUserStatusUseCase toggleUserStatusUseCase;
  private final DeleteUserUseCase deleteUserUseCase;
  private final JpaUserSearch search;

  public UserController(
      CreateUserUseCase createUserUseCase,
      UpdateUserUseCase updateUserUseCase,
      ListUsersUseCase listUsersUseCase,
      ToggleUserStatusUseCase toggleUserStatusUseCase,
      DeleteUserUseCase deleteUserUseCase,
      JpaUserSearch search) {
    this.createUserUseCase = createUserUseCase;
    this.updateUserUseCase = updateUserUseCase;
    this.listUsersUseCase = listUsersUseCase;
    this.toggleUserStatusUseCase = toggleUserStatusUseCase;
    this.deleteUserUseCase = deleteUserUseCase;
    this.search = search;
  }

  @GetMapping(params = "!page")
  public ResponseEntity<List<UserResponseDto>> getAllUsers() {
    return ResponseEntity.ok(listUsersUseCase.execute());
  }

  @GetMapping(params = "page")
  public PageResponse<UserResponseDto> page(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String fullName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long roleId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,desc") String sort) {
    Boolean active = null;
    if (status != null && !status.isBlank()) {
      active = switch (status) {
        case "ACTIVE" -> true;
        case "INACTIVE" -> false;
        default -> throw new com.sakurastore.backend.domain.exception.DomainException("Estado de usuario inválido.");
      };
    }
    return search.search(null, username, fullName, email, null, active, page, size, sort, roleId);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(listUsersUseCase.findById(id));
  }

  @GetMapping("/page")
  public PageResponse<UserResponseDto> search(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String fullName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,desc") String sort) {
    return search.search(id, username, fullName, email, role, active, page, size, sort);
  }

  @PostMapping
  public ResponseEntity<UserResponseDto> createUser(
      @jakarta.validation.Valid @RequestBody CreateUserCommand command) {
    UserResponseDto created = createUserUseCase.execute(command);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserResponseDto> updateUser(
      @PathVariable Long id,
      @jakarta.validation.Valid @RequestBody UpdateUserCommand command,
      org.springframework.security.core.Authentication authentication) {
    UserResponseDto updated =
        updateUserUseCase.execute(
            id, command, authentication.getAuthorities().iterator().next().getAuthority());
    return ResponseEntity.ok(updated);
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<UserResponseDto> toggleUserStatus(
      @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
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
