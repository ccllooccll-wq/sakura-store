package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.infrastructure.persistence.entity.AuthChallengeJpaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AuthChallengeJpaRepository extends JpaRepository<AuthChallengeJpaEntity, UUID> {
  interface Owner {
    Long getUserId();
  }

  Optional<Owner> findOwnerById(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AuthChallengeJpaEntity> findLockedById(UUID id);

  long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

  List<AuthChallengeJpaEntity> findAllByUserIdAndPurposeAndUsedFalse(Long userId, String purpose);

  List<AuthChallengeJpaEntity> findAllByUserIdAndUsedFalse(Long userId);
}
