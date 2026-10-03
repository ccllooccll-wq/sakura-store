package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
  Optional<UserJpaEntity> findByUsername(String username);

  Optional<UserJpaEntity> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);
}
