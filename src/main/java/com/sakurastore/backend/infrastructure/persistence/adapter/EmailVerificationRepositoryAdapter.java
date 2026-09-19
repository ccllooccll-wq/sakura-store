package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.domain.model.EmailVerification;
import com.sakurastore.backend.domain.port.EmailVerificationRepositoryPort;
import com.sakurastore.backend.infrastructure.persistence.entity.EmailVerificationJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.repository.EmailVerificationJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class EmailVerificationRepositoryAdapter implements EmailVerificationRepositoryPort {

    private final EmailVerificationJpaRepository repository;

    public EmailVerificationRepositoryAdapter(EmailVerificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EmailVerification save(EmailVerification domain) {
        EmailVerificationJpaEntity entity = toJpaEntity(domain);
        EmailVerificationJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailVerification> findLatestByUserId(Long userId) {
        return repository.findFirstByUserIdAndUsadoFalseOrderByCreatedAtDesc(userId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void invalidateAllPendingByUserId(Long userId) {
        repository.invalidateAllPendingByUserId(userId);
    }

    private EmailVerificationJpaEntity toJpaEntity(EmailVerification domain) {
        if (domain == null) return null;
        EmailVerificationJpaEntity entity = new EmailVerificationJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setCodeHash(domain.getCodeHash());
        entity.setExpirationDate(domain.getExpirationDate());
        entity.setIntentos(domain.getAttempts());
        entity.setUsado(domain.isUsed());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private EmailVerification toDomain(EmailVerificationJpaEntity entity) {
        if (entity == null) return null;
        return new EmailVerification(
                entity.getId(),
                entity.getUserId(),
                entity.getCodeHash(),
                entity.getExpirationDate(),
                entity.getIntentos(),
                entity.isUsado(),
                entity.getCreatedAt()
        );
    }
}
