package com.sakurastore.backend.infrastructure.security;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = DomainException.class)
public class SessionAuthService {
  private final UserRepositoryPort users;
  private final RoleRepositoryPort roles;
  private final JdbcTemplate db;
  private final PasswordEncoder encoder;
  private final EmailSenderPort mail;
  private final SecureRandom random = new SecureRandom();

  public record Challenge(UUID id, Long userId, String email, String purpose) {}

  private record Stored(
      UUID id,
      Long userId,
      String hash,
      String purpose,
      int attempts,
      boolean used,
      LocalDateTime expires) {}

  public SessionAuthService(
      UserRepositoryPort users,
      RoleRepositoryPort roles,
      JdbcTemplate db,
      PasswordEncoder encoder,
      EmailSenderPort mail) {
    this.users = users;
    this.roles = roles;
    this.db = db;
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
    String email = command.getEmail().trim().toLowerCase(Locale.ROOT),
        username = command.getUsername().trim();
    if (users.existsByEmail(email) || users.existsByUsername(username))
      throw new DomainException("No se pudo registrar. Revisa los datos o recupera tu cuenta.");
    var role =
        roles
            .findByName(RoleEnum.ROLE_VENDEDOR)
            .orElseThrow(() -> new DomainException("No está configurado el rol de registro."));
    var user =
        users.save(
            User.createNewUser(
                username, command.getNombre(), email, encoder.encode(command.getPassword()), role));
    return issue(user, "REGISTER");
  }

  public Challenge requestReset(String email) {
    var user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    return user.filter(User::isActive).map(u -> issue(u, "RESET")).orElse(null);
  }

  public Challenge resend(UUID id, String email) {
    var old = load(id);
    var user =
        users
            .findById(old.userId())
            .orElseThrow(() -> new DomainException("Reinicia el proceso de acceso."));
    if (!user.isActive() || !user.getEmail().equalsIgnoreCase(email.trim()) || old.used())
      throw new DomainException("Reinicia el proceso de acceso.");
    return issue(user, old.purpose());
  }

  private Challenge issue(User user, String purpose) {
    db.queryForObject(
            "SELECT id_usuario FROM usuario WHERE id_usuario = ? FOR UPDATE",
            Long.class,
            user.getId()
    );
    Integer recent =
        db.queryForObject(
            "select count(*) from auth_challenges where user_id=? and"
                + " created_at>current_timestamp-interval '60 seconds'",
            Integer.class,
            user.getId());
    Integer hourly =
        db.queryForObject(
            "select count(*) from auth_challenges where user_id=? and"
                + " created_at>current_timestamp-interval '1 hour'",
            Integer.class,
            user.getId());
    if (recent > 0 || hourly >= 10)
      throw new DomainException(
          "Espera antes de solicitar otro código. Máximo 10 envíos por hora.");
    String code = String.format("%06d", random.nextInt(1000000));
    UUID id = UUID.randomUUID();
    db.update(
        "update auth_challenges set used=true where user_id=? and purpose=? and used=false",
        user.getId(),
        purpose);
    db.update(
        "insert into auth_challenges(id,user_id,code_hash,purpose,expires_at) values(?,?,?,?,?)",
        id,
        user.getId(),
        encoder.encode(code),
        purpose,
        LocalDateTime.now().plusMinutes(10));
    mail.sendVerificationCode(user.getEmail(), user.getFullName(), code);
    return new Challenge(id, user.getId(), user.getEmail(), purpose);
  }

  private Stored load(UUID id) {
    if (id == null)
      throw new DomainException(
          "Inicia el proceso en este navegador antes de introducir el código.");
    return db
        .query(
            "select * from auth_challenges where id=? for update",
            (rs, n) ->
                new Stored(
                    rs.getObject("id", UUID.class),
                    rs.getLong("user_id"),
                    rs.getString("code_hash"),
                    rs.getString("purpose"),
                    rs.getInt("attempts"),
                    rs.getBoolean("used"),
                    rs.getTimestamp("expires_at").toLocalDateTime()),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new DomainException("Código no disponible. Reinicia el proceso."));
  }

  public User verify(UUID id, String email, String code, boolean reset, String newPassword) {
    var challenge = load(id);
    if (reset != challenge.purpose().equals("RESET"))
      throw new DomainException("El código no corresponde a esta operación.");
    if (challenge.used()
        || challenge.attempts() >= 5
        || challenge.expires().isBefore(LocalDateTime.now()))
      throw new DomainException(
          "Código vencido, utilizado o sin intentos disponibles. Solicita otro.");
    db.update("update auth_challenges set attempts=attempts+1 where id=?", id);
    var user =
        users
            .findById(challenge.userId())
            .orElseThrow(() -> new DomainException("Código inválido."));
    if (!user.isActive()
        || !user.getEmail().equalsIgnoreCase(email.trim())
        || !encoder.matches(code, challenge.hash()))
      throw new DomainException("Código incorrecto.");
    db.update("update auth_challenges set used=true where id=?", id);
    if (reset) {
      user.changePassword(encoder.encode(newPassword));
      db.update("update auth_challenges set used=true where user_id=?", user.getId());
    }
    user.markEmailAsVerified();
    return users.save(user);
  }
}
