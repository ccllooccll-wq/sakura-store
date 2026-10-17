package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.SaleJpaEntity;
import org.springframework.data.jpa.repository.*;

public interface SaleJpaRepository
    extends JpaRepository<SaleJpaEntity, Long>, JpaSpecificationExecutor<SaleJpaEntity> {}
