package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.domain.model.RoleEnum;
import com.sakurastore.backend.infrastructure.persistence.entity.RoleJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRoleRepository
        extends JpaRepository<RoleJpaEntity, Long> {

  Optional<RoleJpaEntity> findByName(RoleEnum name);

  List<RoleJpaEntity> findAllByNameIn(List<RoleEnum> names);
}