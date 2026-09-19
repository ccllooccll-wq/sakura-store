package com.sakurastore.backend.infrastructure.persistence.mapper;

import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.infrastructure.persistence.entity.RoleJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.entity.UserJpaEntity;

public class UserPersistenceMapper {

    public static Role toDomainRole(RoleJpaEntity entity) {
        if (entity == null) return null;
        return new Role(entity.getId(), entity.getName(), entity.getDescription());
    }

    public static RoleJpaEntity toJpaRole(Role domain) {
        if (domain == null) return null;
        return new RoleJpaEntity(domain.getId(), domain.getName(), domain.getDescription());
    }

    public static User toDomainUser(UserJpaEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPassword(),
                toDomainRole(entity.getRole()),
                entity.isActive(),
                entity.isEmailVerified(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static UserJpaEntity toJpaUser(User domain) {
        if (domain == null) return null;
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setUsername(domain.getUsername());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setPassword(domain.getPassword());
        entity.setRole(toJpaRole(domain.getRole()));
        entity.setActive(domain.isActive());
        entity.setEmailVerified(domain.isEmailVerified());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
