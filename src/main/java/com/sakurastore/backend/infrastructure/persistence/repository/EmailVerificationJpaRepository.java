package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.EmailVerificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationJpaEntity, Long> {

    Optional<EmailVerificationJpaEntity> findFirstByUserIdAndUsadoFalseOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE EmailVerificationJpaEntity e SET e.usado = true WHERE e.userId = :userId AND e.usado = false")
    void invalidateAllPendingByUserId(@Param("userId") Long userId);
}
