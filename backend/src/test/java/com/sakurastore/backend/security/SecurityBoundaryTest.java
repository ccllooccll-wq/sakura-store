package com.sakurastore.backend.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.application.usecase.*;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import com.sakurastore.backend.infrastructure.config.SecurityConfig;
import com.sakurastore.backend.infrastructure.security.*;
import com.sakurastore.backend.presentation.controller.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  AuthController.class,
  UserController.class,
  InventoryController.class,
  RoleController.class
})
@Import({SecurityConfig.class, SessionUserFilter.class, AuthRateLimitFilter.class})
class SecurityBoundaryTest {
  @Autowired MockMvc mvc;
  @MockBean SessionAuthService auth;
  @MockBean UserRepositoryPort users;
  @MockBean RoleRepositoryPort roles;
  @MockBean InventoryService inventory;
  @MockBean CreateUserUseCase create;
  @MockBean UpdateUserUseCase update;
  @MockBean ListUsersUseCase list;
  @MockBean ToggleUserStatusUseCase toggle;
  @MockBean DeleteUserUseCase delete;
  @MockBean com.sakurastore.backend.infrastructure.persistence.adapter.JpaUserSearch search;
  @MockBean com.sakurastore.backend.application.port.InventoryRepositoryPort inventoryRepository;

  @Test
  void anonymousCannotListUsers() throws Exception {
    mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousCannotReadInventory() throws Exception {
    mvc.perform(get("/api/inventory/products")).andExpect(status().isUnauthorized());
  }

  @Test
  void forgedAdminHeaderDoesNotAuthenticate() throws Exception {
    mvc.perform(get("/api/users").header("X-User-Role", "ROLE_ADMIN"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void vendorCannotEditUsers() throws Exception {
    mvc.perform(
            put("/api/users/1")
                .with(user("vendor").roles("VENDEDOR"))
                .with(csrf())
                .contentType("application/json")
                .content(
"""
{"fullName":"Test","email":"test@example.com","roleId":1}
""")
                .header("X-User-Role", "ROLE_ADMIN"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(update);
  }

  @Test
  void vendorCanReadInventory() throws Exception {
    when(inventory.products("", false)).thenReturn(List.of());
    mvc.perform(get("/api/inventory/products").with(user("vendor").roles("VENDEDOR")))
        .andExpect(status().isOk());
  }

  @Test
  void vendorCannotWriteInventory() throws Exception {
    mvc.perform(
            post("/api/inventory/categories")
                .with(user("vendor").roles("VENDEDOR"))
                .with(csrf())
                .contentType("application/json")
                .content(
"""
{"name":"Prueba"}
"""))
        .andExpect(status().isForbidden());
  }

  @Test
  void providerCannotMutateInventory() throws Exception {
    for (String role : List.of("PROVEEDOR", "ALMACENERO", "VENDEDOR")) {
      for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
        mvc.perform(
                request(
                        org.springframework.http.HttpMethod.valueOf(method),
                        "/api/inventory/products/1")
                    .with(user("test").roles(role))
                    .with(csrf())
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().isForbidden());
      }
    }
    verifyNoInteractions(inventory);
  }

  @Test
  void adminCanCreateCategory() throws Exception {
    mvc.perform(
            post("/api/inventory/categories")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"Prueba\"}"))
        .andExpect(status().isCreated());
    verify(inventory).category(any());
  }

  @Test
  void csrfIsRequiredEvenForLogin() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
"""
{"username":"admin","password":"sample-password"}
"""))
        .andExpect(status().isForbidden());
  }

  @Test
  void csrfEndpointAvailable() throws Exception {
    mvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void passwordAloneDoesNotAuthenticate() throws Exception {
    when(auth.login(any()))
        .thenReturn(
            new SessionAuthService.Challenge(UUID.randomUUID(), 1L, "test@example.com", "LOGIN"));
    var response =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
"""
{"username":"testuser","password":"sample-password"}
"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiresOtp").value(true))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andReturn();
    var session = (MockHttpSession) response.getRequest().getSession();
    mvc.perform(get("/api/users").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void verifiedCodeCreatesServerSessionAndLogoutInvalidatesIt() throws Exception {
    var u =
        User.createNewUser(
            "testuser",
            "Test",
            "test@example.com",
            "HASH",
            new Role(1L, RoleEnum.ROLE_ADMIN, "Admin"));
    u.setId(1L);
    u.markEmailAsVerified();
    when(auth.verify(any(), anyString(), anyString(), eq(false), isNull())).thenReturn(u);
    when(users.findById(1L)).thenReturn(Optional.of(u));
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("LOGIN_CHALLENGE", UUID.randomUUID());
    String before = session.getId();
    mvc.perform(
            post("/api/auth/verificar-email")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(
"""
{"email":"test@example.com","codigo":"123456"}
"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.username").value("testuser"))
        .andExpect(jsonPath("$.token").doesNotExist());
    assertNotEquals(before, session.getId());
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
    mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertTrue(session.isInvalid());
  }
}
