package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface ProductJpaRepository
    extends JpaRepository<ProductJpaEntity, Long>, JpaSpecificationExecutor<ProductJpaEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ProductJpaEntity> findLockedById(Long id);
}
