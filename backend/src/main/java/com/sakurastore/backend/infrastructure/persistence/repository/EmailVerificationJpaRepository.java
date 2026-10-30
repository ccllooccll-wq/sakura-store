package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.EmailVerificationJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationJpaRepository
    extends JpaRepository<EmailVerificationJpaEntity, Long> {

  Optional<EmailVerificationJpaEntity> findFirstByUserIdAndUsadoFalseOrderByCreatedAtDesc(
      Long userId);

  java.util.List<EmailVerificationJpaEntity> findAllByUserIdAndUsadoFalse(Long userId);

  @org.springframework.transaction.annotation.Transactional
  default void invalidateAllPendingByUserId(Long userId) {
    findAllByUserIdAndUsadoFalse(userId).forEach(pending -> pending.setUsado(true));
  }
}
