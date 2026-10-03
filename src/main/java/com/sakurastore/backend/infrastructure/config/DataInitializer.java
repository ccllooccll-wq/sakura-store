package com.sakurastore.backend.infrastructure.config;

import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private final RoleRepositoryPort roles;
  private final UserRepositoryPort users;
  private final PasswordEncoder encoder;

  @Value("${app.bootstrap.email:}")
  private String email;

  @Value("${app.bootstrap.password:}")
  private String password;

  public DataInitializer(
      RoleRepositoryPort roles, UserRepositoryPort users, PasswordEncoder encoder) {
    this.roles = roles;
    this.users = users;
    this.encoder = encoder;
  }

  @Override
  public void run(String... args) {
    for (var name : RoleEnum.values())
      if (roles.findByName(name).isEmpty())
        roles.save(new Role(name, name.name().replace("ROLE_", "")));
    if (!email.isBlank()
        && !password.isBlank()
        && users.findAll().stream().noneMatch(u -> u.getRole().getName() == RoleEnum.ROLE_ADMIN)) {
      if (password.length() < 12 || password.length() > 72)
        throw new IllegalStateException("ADMIN_PASSWORD debe tener entre 12 y 72 caracteres.");
      users.save(
          User.createNewUser(
              "admin",
              "Administrador",
              email,
              encoder.encode(password),
              roles.findByName(RoleEnum.ROLE_ADMIN).orElseThrow()));
    }
  }
}
//modificado por derik