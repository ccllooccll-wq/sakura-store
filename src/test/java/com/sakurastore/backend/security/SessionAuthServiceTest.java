package com.sakurastore.backend.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import com.sakurastore.backend.infrastructure.persistence.entity.AuthChallengeJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.repository.AuthChallengeJpaRepository;
import com.sakurastore.backend.infrastructure.security.SessionAuthService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class SessionAuthServiceTest {
  UserRepositoryPort users = mock(UserRepositoryPort.class);
  RoleRepositoryPort roles = mock(RoleRepositoryPort.class);
  AuthChallengeJpaRepository challenges = mock(AuthChallengeJpaRepository.class);
  PasswordEncoder encoder = mock(PasswordEncoder.class);
  EmailSenderPort mail = mock(EmailSenderPort.class);
  SessionAuthService service = new SessionAuthService(users, roles, challenges, encoder, mail);
  UUID id = UUID.randomUUID();
  AuthChallengeJpaEntity stored;

  void challenge(String purpose, int attempts, boolean used, boolean expired) {
    stored = new AuthChallengeJpaEntity();
    stored.setId(id);
    stored.setUserId(1L);
    stored.setCodeHash("HASH");
    stored.setPurpose(purpose);
    stored.setAttempts(attempts);
    stored.setUsed(used);
    stored.setExpiresAt(LocalDateTime.now().plusMinutes(expired ? -1 : 10));
    when(challenges.findOwnerById(id)).thenReturn(Optional.of(() -> 1L));
    when(challenges.findLockedById(id)).thenReturn(Optional.of(stored));
    var user =
        User.createNewUser(
            "testuser",
            "Test",
            "test@example.com",
            "PASSWORD_HASH",
            new Role(2L, RoleEnum.ROLE_CLIENTE, "Cliente"));
    user.setId(1L);
    when(users.findById(1L)).thenReturn(Optional.of(user));
    when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
    when(users.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void cannotVerifyWithoutBrowserChallenge() {
    assertThrows(
        DomainException.class,
        () -> service.verify(null, "test@example.com", "123456", false, null));
  }

  @Test
  void wrongPurposeRejected() {
    challenge("RESET", 0, false, false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
    verify(users, never()).save(any());
  }

  @Test
  void fiveAttemptsRejected() {
    challenge("LOGIN", 5, false, false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
    verify(encoder, never()).matches(any(), any());
  }

  @Test
  void usedRejected() {
    challenge("LOGIN", 0, true, false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
  }

  @Test
  void expiredRejected() {
    challenge("LOGIN", 0, false, true);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
  }

  @Test
  void wrongCodeConsumesAttempt() {
    challenge("LOGIN", 0, false, false);
    when(encoder.matches("000000", "HASH")).thenReturn(false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "000000", false, null));
    assertEquals(1, stored.getAttempts());
    verify(users, never()).save(any());
  }

  @Test
  void successfulCodeConsumed() {
    challenge("LOGIN", 0, false, false);
    when(encoder.matches("123456", "HASH")).thenReturn(true);
    assertTrue(service.verify(id, "test@example.com", "123456", false, null).isEmailVerified());
    assertTrue(stored.isUsed());
  }
}
