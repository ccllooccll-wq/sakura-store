package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.PurchaseJpaEntity;
import org.springframework.data.jpa.repository.*;

public interface PurchaseJpaRepository
    extends JpaRepository<PurchaseJpaEntity, Long>, JpaSpecificationExecutor<PurchaseJpaEntity> {}
