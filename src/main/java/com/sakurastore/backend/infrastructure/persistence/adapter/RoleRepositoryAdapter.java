package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.RoleEnum;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.infrastructure.persistence.entity.RoleJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.sakurastore.backend.infrastructure.persistence.repository.SpringDataRoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RoleRepositoryAdapter implements RoleRepositoryPort {
    private final SpringDataRoleRepository springDataRoleRepository;

    public RoleRepositoryAdapter(SpringDataRoleRepository springDataRoleRepository) {
        this.springDataRoleRepository = springDataRoleRepository;
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = UserPersistenceMapper.toJpaRole(role);
        RoleJpaEntity saved = springDataRoleRepository.save(entity);
        return UserPersistenceMapper.toDomainRole(saved);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return springDataRoleRepository.findById(id)
                .map(UserPersistenceMapper::toDomainRole);
    }

    @Override
    public Optional<Role> findByName(RoleEnum name) {
        return springDataRoleRepository.findByName(name)
                .map(UserPersistenceMapper::toDomainRole);
    }

    @Override
    public List<Role> findAll() {
        return springDataRoleRepository.findAll().stream()
                .map(UserPersistenceMapper::toDomainRole)
                .collect(Collectors.toList());
    }
}
