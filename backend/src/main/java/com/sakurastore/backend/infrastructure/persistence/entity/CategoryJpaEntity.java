package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
public class CategoryJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_categoria")
  private Long id;

  @Column(name = "nombre", nullable = false, length = 100)
  private String name;

  @Column(name = "descripcion", length = 255)
  private String description;

  @Column(name = "activo", nullable = false)
  private boolean active = true;
}
