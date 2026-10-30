package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_challenges")
@Getter
@Setter
@NoArgsConstructor
public class AuthChallengeJpaEntity {
  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "code_hash", nullable = false, length = 255)
  private String codeHash;

  @Column(nullable = false, length = 10)
  private String purpose;

  @Column(nullable = false)
  private int attempts;

  @Column(nullable = false)
  private boolean used;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;
}
