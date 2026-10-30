package com.sakurastore.backend.infrastructure.security;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import com.sakurastore.backend.infrastructure.persistence.entity.AuthChallengeJpaEntity;
import com.sakurastore.backend.infrastructure.persistence.repository.AuthChallengeJpaRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = DomainException.class)
public class SessionAuthService {
  private final UserRepositoryPort users;
  private final RoleRepositoryPort roles;
  private final AuthChallengeJpaRepository challenges;
  private final PasswordEncoder encoder;
  private final EmailSenderPort mail;
  private final SecureRandom random = new SecureRandom();

  public record Challenge(UUID id, Long userId, String email, String purpose) {}

  public SessionAuthService(
      UserRepositoryPort users,
      RoleRepositoryPort roles,
      AuthChallengeJpaRepository challenges,
      PasswordEncoder encoder,
      EmailSenderPort mail) {
    this.users = users;
    this.roles = roles;
    this.challenges = challenges;
    this.encoder = encoder;
    this.mail = mail;
  }

  public Challenge login(LoginCommand command) {
    String input = command.getUsername().trim();
    var user =
        users
            .findByUsername(input)
            .or(() -> users.findByEmail(input.toLowerCase(Locale.ROOT)))
            .orElseThrow(() -> new DomainException("Usuario o contraseña incorrectos."));
    if (!user.isActive() || !encoder.matches(command.getPassword(), user.getPassword()))
      throw new DomainException("Usuario o contraseña incorrectos.");
    return issue(user, "LOGIN");
  }

  public Challenge register(RegisterUserCommand command) {
    String email = command.getEmail().trim().toLowerCase(Locale.ROOT);
    String username = command.getUsername().trim();
    if (users.existsByEmail(email) || users.existsByUsername(username))
      throw new DomainException("No se pudo registrar. Revisa los datos o recupera tu cuenta.");
    // El rol público lo decide el servidor. No se recibe un rol del navegador.
    var role =
        roles
            .findByName(RoleEnum.ROLE_CLIENTE)
            .orElseThrow(() -> new DomainException("No está configurado el rol de cliente."));
    var user =
        users.save(
            User.createNewUser(
                username, command.getNombre(), email, encoder.encode(command.getPassword()), role));
    return issue(user, "REGISTER");
  }

  public Challenge requestReset(String email) {
    return users
        .findByEmail(email.trim().toLowerCase(Locale.ROOT))
        .filter(User::isActive)
        .map(user -> issue(user, "RESET"))
        .orElse(null);
  }

  public Challenge resend(UUID id, String email) {
    var old = load(id);
    var user =
        users
            .findById(old.getUserId())
            .orElseThrow(() -> new DomainException("Reinicia el proceso de acceso."));
    if (!user.isActive() || !user.getEmail().equalsIgnoreCase(email.trim()) || old.isUsed())
      throw new DomainException("Reinicia el proceso de acceso.");
    return issue(user, old.getPurpose());
  }

  private Challenge issue(User user, String purpose) {
    // Todos los cambios de códigos de una cuenta toman primero este mismo bloqueo.
    var current =
        users
            .findByIdForUpdate(user.getId())
            .orElseThrow(() -> new DomainException("La cuenta no está disponible."));
    if (!current.isActive()) throw new DomainException("La cuenta no está disponible.");
    LocalDateTime now = LocalDateTime.now();
    long recent = challenges.countByUserIdAndCreatedAtAfter(user.getId(), now.minusSeconds(60));
    long hourly = challenges.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(1));
    if (recent > 0 || hourly >= 10)
      throw new DomainException(
          "Espera antes de solicitar otro código. Máximo 10 envíos por hora.");
    challenges
        .findAllByUserIdAndPurposeAndUsedFalse(user.getId(), purpose)
        .forEach(challenge -> challenge.setUsed(true));
    String code = String.format("%06d", random.nextInt(1000000));
    var challenge = new AuthChallengeJpaEntity();
    challenge.setId(UUID.randomUUID());
    challenge.setUserId(user.getId());
    challenge.setCodeHash(encoder.encode(code));
    challenge.setPurpose(purpose);
    challenge.setCreatedAt(now);
    challenge.setExpiresAt(now.plusMinutes(10));
    challenges.saveAndFlush(challenge);
    mail.sendVerificationCode(current.getEmail(), current.getFullName(), code);
    return new Challenge(challenge.getId(), current.getId(), current.getEmail(), purpose);
  }

  private AuthChallengeJpaEntity load(UUID id) {
    if (id == null)
      throw new DomainException(
          "Inicia el proceso en este navegador antes de introducir el código.");
    var preview =
        challenges
            .findOwnerById(id)
            .orElseThrow(() -> new DomainException("Código no disponible. Reinicia el proceso."));
    // Mismo orden de bloqueo que issue(): usuario y después desafío.
    users
        .findByIdForUpdate(preview.getUserId())
        .orElseThrow(() -> new DomainException("La cuenta no está disponible."));
    return challenges
        .findLockedById(id)
        .orElseThrow(() -> new DomainException("Código no disponible. Reinicia el proceso."));
  }

  public User verify(UUID id, String email, String code, boolean reset, String newPassword) {
    var challenge = load(id);
    if (reset != challenge.getPurpose().equals("RESET"))
      throw new DomainException("El código no corresponde a esta operación.");
    if (challenge.isUsed()
        || challenge.getAttempts() >= 5
        || !challenge.getExpiresAt().isAfter(LocalDateTime.now()))
      throw new DomainException(
          "Código vencido, utilizado o sin intentos disponibles. Solicita otro.");
    challenge.setAttempts(challenge.getAttempts() + 1);
    var user =
        users
            .findById(challenge.getUserId())
            .orElseThrow(() -> new DomainException("Código inválido."));
    if (!user.isActive()
        || !user.getEmail().equalsIgnoreCase(email.trim())
        || !encoder.matches(code, challenge.getCodeHash()))
      throw new DomainException("Código incorrecto.");
    challenge.setUsed(true);
    if (reset) {
      user.changePassword(encoder.encode(newPassword));
      challenges
          .findAllByUserIdAndUsedFalse(user.getId())
          .forEach(pending -> pending.setUsed(true));
    }
    user.markEmailAsVerified();
    return users.save(user);
  }
}
