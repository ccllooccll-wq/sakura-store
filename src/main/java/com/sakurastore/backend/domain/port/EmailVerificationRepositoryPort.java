package com.sakurastore.backend.domain.port;

import com.sakurastore.backend.domain.model.EmailVerification;

import java.util.Optional;

public interface EmailVerificationRepositoryPort {
    EmailVerification save(EmailVerification verification);
    Optional<EmailVerification> findLatestByUserId(Long userId);
    void invalidateAllPendingByUserId(Long userId);
}
