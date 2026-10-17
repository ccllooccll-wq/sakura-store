package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.MovementJpaEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementJpaRepository extends JpaRepository<MovementJpaEntity, Long> {
  Page<MovementJpaEntity> findAllByProductId(Long productId, Pageable pageable);
}
