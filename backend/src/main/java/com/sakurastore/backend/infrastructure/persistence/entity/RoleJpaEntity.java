package com.sakurastore.backend.infrastructure.persistence.entity;

import com.sakurastore.backend.domain.model.RoleEnum;
import jakarta.persistence.*;

@Entity
@Table(name = "rol")
public class RoleJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_rol")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "nombre", nullable = false, unique = true, length = 50)
  private RoleEnum name;

  @Transient
  private String description;

  public RoleJpaEntity() {
  }

  public RoleJpaEntity(Long id, RoleEnum name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RoleEnum getName() {
    return name;
  }

  public void setName(RoleEnum name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}