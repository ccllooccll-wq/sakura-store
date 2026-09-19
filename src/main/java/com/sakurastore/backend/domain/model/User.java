package com.sakurastore.backend.domain.model;

import com.sakurastore.backend.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String password;
    private Role role;
    private boolean active;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
        this.active = true;
        this.emailVerified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(Long id, String username, String fullName, String email, String password, Role role, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, fullName, email, password, role, active, false, createdAt, updatedAt);
    }

    public User(Long id, String username, String fullName, String email, String password, Role role, boolean active, boolean emailVerified, LocalDateTime createdAt, LocalDateTime updatedAt) {
        validateUsername(username);
        validateFullName(fullName);
        validateEmail(email);
        validatePassword(password);
        validateRole(role);

        this.id = id;
        this.username = username.trim();
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.password = password;
        this.role = role;
        this.active = active;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public static User createNewUser(String username, String fullName, String email, String password, Role role) {
        return new User(null, username, fullName, email, password, role, true, false, LocalDateTime.now(), LocalDateTime.now());
    }

    public void markEmailAsVerified() {
        this.emailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void activar() {
        if (this.active) {
            throw new DomainException("El usuario ya se encuentra activo.");
        }
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void desactivar() {
        if (!this.active) {
            throw new DomainException("El usuario ya se encuentra inactivo.");
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(String fullName, String email, Role role) {
        validateFullName(fullName);
        validateEmail(email);
        validateRole(role);

        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.role = role;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String newPassword) {
        validatePassword(newPassword);
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new DomainException("El nombre de usuario es obligatorio.");
        }
        if (username.trim().length() < 3) {
            throw new DomainException("El nombre de usuario debe tener al menos 3 caracteres.");
        }
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new DomainException("El nombre completo es obligatorio.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new DomainException("El correo electrónico es obligatorio.");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new DomainException("El formato del correo electrónico es inválido.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new DomainException("La contraseña es obligatoria.");
        }
    }

    private void validateRole(Role role) {
        if (role == null) {
            throw new DomainException("El rol asignado al usuario es obligatorio.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
