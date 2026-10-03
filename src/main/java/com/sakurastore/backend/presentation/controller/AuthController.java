package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.port.UserRepositoryPort;
import com.sakurastore.backend.infrastructure.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {
  private final SessionAuthService service;
  private final UserRepositoryPort users;
  private final SecurityContextRepository contexts;

  public AuthController(
      SessionAuthService service, UserRepositoryPort users, SecurityContextRepository contexts) {
    this.service = service;
    this.users = users;
    this.contexts = contexts;
  }

  @GetMapping("/api/auth/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/api/auth/me")
  public UserResponseDto me(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof SessionIdentity id))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión.");
    return UserResponseDto.fromDomain(
        users
            .findById(id.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
  }

  @PostMapping("/api/auth/login")
  public Map<String, Object> login(
      @Valid @RequestBody LoginCommand command, HttpServletRequest req) {
    return bind(service.login(command), req, "LOGIN_CHALLENGE");
  }

  @PostMapping({"/api/usuarios/registro", "/api/users/registro", "/api/auth/registro"})
  public Map<String, Object> register(
      @Valid @RequestBody RegisterUserCommand command, HttpServletRequest req) {
    return bind(service.register(command), req, "LOGIN_CHALLENGE");
  }

  private Map<String, Object> bind(
      SessionAuthService.Challenge c, HttpServletRequest req, String attribute) {
    req.getSession(true).setAttribute(attribute, c.id());
    return Map.of(
        "requiresOtp",
        true,
        "email",
        c.email(),
        "mensaje",
        "Revisa tu correo. El código vence en 10 minutos.");
  }

  @PostMapping("/api/auth/reenviar-codigo")
  public Map<String, Object> resend(
      @Valid @RequestBody ResendCodeCommand c, HttpServletRequest req) {
    return bind(
        service.resend(challenge(req, "LOGIN_CHALLENGE"), c.getEmail()), req, "LOGIN_CHALLENGE");
  }

  @PostMapping("/api/auth/verificar-email")
  public Map<String, Object> verify(
      @Valid @RequestBody VerifyEmailCommand c, HttpServletRequest req, HttpServletResponse res) {
    var u =
        service.verify(challenge(req, "LOGIN_CHALLENGE"), c.getEmail(), c.getCodigo(), false, null);
    req.getSession().removeAttribute("LOGIN_CHALLENGE");
    req.getSession().removeAttribute("RESET_CHALLENGE");
    req.changeSessionId();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            new SessionIdentity(u.getId(), u.getUsername(), u.getPassword()),
            null,
            List.of(new SimpleGrantedAuthority(u.getRole().getName().name())));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, req, res);
    new HttpSessionCsrfTokenRepository().saveToken(null, req, res);
    return Map.of(
        "user", UserResponseDto.fromDomain(u), "mensaje", "Sesión iniciada correctamente.");
  }

  @PostMapping("/api/auth/solicitar-recuperacion")
  public ApiResponseDto recover(
      @Valid @RequestBody RequestPasswordResetCommand c, HttpServletRequest req) {
    req.getSession(true).removeAttribute("RESET_CHALLENGE");
    try {
      var challenge = service.requestReset(c.getEmail());
      if (challenge != null) req.getSession().setAttribute("RESET_CHALLENGE", challenge.id());
    } catch (DomainException ignored) {
      /* Same response for account existence and account-specific throttling. */
    }
    return new ApiResponseDto(
        "Si la cuenta está disponible, recibirás un código. Revisa también la carpeta de spam.");
  }

  @PostMapping("/api/auth/restablecer-password")
  public ApiResponseDto reset(@Valid @RequestBody ResetPasswordCommand c, HttpServletRequest req) {
    service.verify(
        challenge(req, "RESET_CHALLENGE"), c.getEmail(), c.getCodigo(), true, c.getNuevaPassword());
    req.getSession().invalidate();
    SecurityContextHolder.clearContext();
    return new ApiResponseDto("Contraseña actualizada. Inicia sesión nuevamente.");
  }

  private UUID challenge(HttpServletRequest req, String name) {
    return req.getSession(false) == null ? null : (UUID) req.getSession(false).getAttribute(name);
  }
}
