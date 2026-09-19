package com.sakurastore.backend.domain.port;

import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.RoleEnum;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryPort {
    Role save(Role role);
    Optional<Role> findById(Long id);
    Optional<Role> findByName(RoleEnum name);
    List<Role> findAll();
}
