package com.sakurastore.backend.domain.model;

import com.sakurastore.backend.domain.exception.DomainException;

import java.util.Objects;

public class Role {
    private Long id;
    private RoleEnum name;
    private String description;

    public Role() {
    }

    public Role(Long id, RoleEnum name, String description) {
        if (name == null) {
            throw new DomainException("El nombre del rol no puede ser nulo.");
        }
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Role(RoleEnum name, String description) {
        this(null, name, description);
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
        if (name == null) {
            throw new DomainException("El nombre del rol no puede ser nulo.");
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) && name == role.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
