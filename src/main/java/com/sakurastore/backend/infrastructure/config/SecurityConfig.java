package com.sakurastore.backend.infrastructure.config;

import com.sakurastore.backend.infrastructure.security.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public SecurityContextRepository contextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      SessionUserFilter users,
      AuthRateLimitFilter rate,
      SecurityContextRepository contexts)
      throws Exception {
    http.securityContext(c -> c.securityContextRepository(contexts).requireExplicitSave(true))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/auth/**", "/api/usuarios/registro", "/api/users/registro")
                    .permitAll()
                    .requestMatchers("/api/users/**", "/api/roles/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/inventory/**")
                    .authenticated()
                    .requestMatchers("/api/inventory/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter().write("{\"message\":\"Inicia sesión para continuar.\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json");
                          res.getWriter()
                              .write(
                                  "{\"message\":\"No tienes permiso o tu sesión de formulario"
                                      + " venció. Recarga la página.\"}");
                        }))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("SAKURA_SESSION")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .addFilterBefore(users, AnonymousAuthenticationFilter.class)
        .addFilterBefore(rate, SessionUserFilter.class);
    return http.build();
  }

  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<SessionUserFilter>
      noAutoUserFilter(SessionUserFilter f) {
    var r = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
    r.setEnabled(false);
    return r;
  }

  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<AuthRateLimitFilter>
      noAutoRateFilter(AuthRateLimitFilter f) {
    var r = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
    r.setEnabled(false);
    return r;
  }
}
