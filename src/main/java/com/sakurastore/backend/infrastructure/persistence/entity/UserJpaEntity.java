package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class UserJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario")
  private Long id;

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "apellido", length = 100)
  private String apellido;

  @Column(name = "email", nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_rol", nullable = false)
  private RoleJpaEntity role;

  @Column(name = "activo", nullable = false)
  private boolean active = true;

  @Column(name = "email_verificado", nullable = false)
  private boolean emailVerified = false;

  @Transient
  private LocalDateTime createdAt;

  @Transient
  private LocalDateTime updatedAt;

  public UserJpaEntity() {
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
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

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFullName() {
    if (apellido == null || apellido.isBlank()) {
      return nombre;
    }
    return nombre + " " + apellido;
  }

  public void setFullName(String fullName) {
    String value = fullName == null ? "" : fullName.trim();
    int espacio = value.indexOf(" ");

    if (espacio < 0) {
      this.nombre = value;
      this.apellido = null;
    } else {
      this.nombre = value.substring(0, espacio);
      this.apellido = value.substring(espacio + 1).trim();
    }
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public RoleJpaEntity getRole() {
    return role;
  }

  public void setRole(RoleJpaEntity role) {
    this.role = role;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}