package com.sakurastore.backend.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  private record Window(long start, int count) {}

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (!req.getMethod().equals("POST")
        || !(req.getRequestURI().startsWith("/api/auth/")
            || req.getRequestURI().endsWith("/registro"))) {
      chain.doFilter(req, res);
      return;
    }
    long now = System.currentTimeMillis();
    windows.entrySet().removeIf(e -> now - e.getValue().start() > 60000);
    String key = req.getRemoteAddr();
    if (windows.size() >= 10000 && !windows.containsKey(key)) {
      res.setStatus(429);
      return;
    }
    var window =
        windows.compute(
            key,
            (k, v) ->
                v == null || now - v.start() > 60000
                    ? new Window(now, 1)
                    : new Window(v.start(), v.count() + 1));
    if (window.count() > 20) {
      res.setStatus(429);
      res.setHeader("Retry-After", "60");
      res.setContentType("application/json");
      res.getWriter().write("{\"message\":\"Demasiados intentos. Espera un minuto.\"}");
      return;
    }
    chain.doFilter(req, res);
  }
}
