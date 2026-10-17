package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.CategoryJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {
  List<CategoryJpaEntity> findAllByActiveTrueOrderByNameAsc();

  boolean existsByIdAndActiveTrue(Long id);
}
