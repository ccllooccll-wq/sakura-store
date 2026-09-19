package com.sakurastore.backend.infrastructure.persistence.entity;

import com.sakurastore.backend.domain.model.RoleEnum;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleEnum name;

    @Column(length = 255)
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
