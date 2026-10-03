package com.sakurastore.backend.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import com.sakurastore.backend.infrastructure.security.SessionAuthService;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class SessionAuthServiceTest {
  UserRepositoryPort users = mock(UserRepositoryPort.class);
  RoleRepositoryPort roles = mock(RoleRepositoryPort.class);
  JdbcTemplate db = mock(JdbcTemplate.class);
  PasswordEncoder encoder = mock(PasswordEncoder.class);
  EmailSenderPort mail = mock(EmailSenderPort.class);
  SessionAuthService service = new SessionAuthService(users, roles, db, encoder, mail);
  UUID id = UUID.randomUUID();

  @SuppressWarnings({"unchecked", "rawtypes"})
  void challenge(String purpose, int attempts, boolean used, boolean expired) throws Exception {
    ResultSet row = mock(ResultSet.class);
    when(row.getObject("id", UUID.class)).thenReturn(id);
    when(row.getLong("user_id")).thenReturn(1L);
    when(row.getString("code_hash")).thenReturn("HASH");
    when(row.getString("purpose")).thenReturn(purpose);
    when(row.getInt("attempts")).thenReturn(attempts);
    when(row.getBoolean("used")).thenReturn(used);
    when(row.getTimestamp("expires_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now().plusMinutes(expired ? -1 : 10)));
    when(db.query(anyString(), any(RowMapper.class), eq(id)))
        .thenAnswer(inv -> List.of(((RowMapper) inv.getArgument(1)).mapRow(row, 0)));
    var u =
        User.createNewUser(
            "testuser",
            "Test",
            "test@example.com",
            "PASSWORD_HASH",
            new Role(2L, RoleEnum.ROLE_VENDEDOR, "Vendedor"));
    u.setId(1L);
    when(users.findById(1L)).thenReturn(Optional.of(u));
    when(users.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void cannotVerifyWithoutBrowserChallenge() {
    assertThrows(
        DomainException.class,
        () -> service.verify(null, "test@example.com", "123456", false, null));
  }

  @Test
  void wrongPurposeRejected() throws Exception {
    challenge("RESET", 0, false, false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
    verify(users, never()).save(any());
  }

  @Test
  void fiveAttemptsRejected() throws Exception {
    challenge("LOGIN", 5, false, false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
    verify(encoder, never()).matches(any(), any());
  }

  @Test
  void usedRejected() throws Exception {
    challenge("LOGIN", 0, true, false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
  }

  @Test
  void expiredRejected() throws Exception {
    challenge("LOGIN", 0, false, true);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "123456", false, null));
  }

  @Test
  void wrongCodeIncrementsAttempts() throws Exception {
    challenge("LOGIN", 0, false, false);
    when(encoder.matches("000000", "HASH")).thenReturn(false);
    assertThrows(
        DomainException.class, () -> service.verify(id, "test@example.com", "000000", false, null));
    verify(db).update("update auth_challenges set attempts=attempts+1 where id=?", id);
    verify(users, never()).save(any());
  }

  @Test
  void successfulCodeConsumed() throws Exception {
    challenge("LOGIN", 0, false, false);
    when(encoder.matches("123456", "HASH")).thenReturn(true);
    assertTrue(service.verify(id, "test@example.com", "123456", false, null).isEmailVerified());
    verify(db).update("update auth_challenges set used=true where id=?", id);
  }
}
