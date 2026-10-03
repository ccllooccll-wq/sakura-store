package com.sakurastore.backend.infrastructure.security;

import com.sakurastore.backend.domain.port.UserRepositoryPort;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionUserFilter extends OncePerRequestFilter {
  private final UserRepositoryPort users;

  public SessionUserFilter(UserRepositoryPort users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof SessionIdentity identity) {
      var current = users.findById(identity.id());
      if (current.isEmpty()
          || !current.get().isActive()
          || !current.get().isEmailVerified()
          || !current.get().getPassword().equals(identity.passwordHash())) {
        SecurityContextHolder.clearContext();
        if (req.getSession(false) != null) req.getSession(false).invalidate();
      } else {
        var u = current.get();
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    identity,
                    null,
                    List.of(new SimpleGrantedAuthority(u.getRole().getName().name()))));
      }
    }
    chain.doFilter(req, res);
  }
}
