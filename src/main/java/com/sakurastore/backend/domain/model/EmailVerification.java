package com.sakurastore.backend.domain.model;

import com.sakurastore.backend.domain.exception.DomainException;

import java.time.LocalDateTime;

public class EmailVerification {
    private Long id;
    private Long userId;
    private String codeHash;
    private LocalDateTime expirationDate;
    private int attempts;
    private boolean used;
    private LocalDateTime createdAt;

    public EmailVerification() {
        this.attempts = 0;
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public EmailVerification(Long id, Long userId, String codeHash, LocalDateTime expirationDate, int attempts, boolean used, LocalDateTime createdAt) {
        if (userId == null) {
            throw new DomainException("El ID del usuario es obligatorio para la verificación.");
        }
        if (codeHash == null || codeHash.isEmpty()) {
            throw new DomainException("El código de verificación no es válido.");
        }
        if (expirationDate == null) {
            throw new DomainException("La fecha de expiración es obligatoria.");
        }

        this.id = id;
        this.userId = userId;
        this.codeHash = codeHash;
        this.expirationDate = expirationDate;
        this.attempts = attempts;
        this.used = used;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static EmailVerification create(Long userId, String codeHash, int expirationMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new EmailVerification(
                null,
                userId,
                codeHash,
                now.plusMinutes(expirationMinutes),
                0,
                false,
                now
        );
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationDate);
    }

    public boolean hasExceededAttempts(int maxAttempts) {
        return this.attempts >= maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsUsed() {
        this.used = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
