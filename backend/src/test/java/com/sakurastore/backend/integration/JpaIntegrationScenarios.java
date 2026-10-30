package com.sakurastore.backend.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.application.dto.CommerceDtos.*;
import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.usecase.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import com.sakurastore.backend.infrastructure.mail.ResendEmailAdapter.MailDeliveryException;
import com.sakurastore.backend.infrastructure.persistence.repository.*;
import com.sakurastore.backend.infrastructure.security.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Casos reutilizados en H2 y PostgreSQL; todas las operaciones de datos pasan por JPA. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class JpaIntegrationScenarios {
  private static final String PASSWORD = "SafePassword123!";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepositoryPort users;
  @Autowired RoleRepositoryPort roles;
  @Autowired PasswordEncoder encoder;
  @Autowired SessionAuthService auth;
  @Autowired InventoryService inventory;
  @Autowired CommerceService commerce;
  @Autowired SpringDataUserRepository userRows;
  @Autowired AuthChallengeJpaRepository challenges;
  @Autowired SaleJpaRepository sales;
  @Autowired PurchaseJpaRepository purchases;
  @Autowired ProductJpaRepository products;
  @Autowired MovementJpaRepository movements;
  @Autowired CategoryJpaRepository categories;
  @Autowired EmailVerificationJpaRepository legacyCodes;
  @MockBean EmailSenderPort mail;
  @org.springframework.boot.test.mock.mockito.SpyBean
  com.sakurastore.backend.infrastructure.persistence.adapter.JpaInventoryRepository inventoryRows;
  User admin;
  User customer;
  User other;
  User seller;
  Category category;
  private String passwordHash;

  @BeforeEach
  void fixtures() {
    sales.deleteAll();
    purchases.deleteAll();
    movements.deleteAll();
    products.deleteAll();
    categories.deleteAll();
    challenges.deleteAll();
    legacyCodes.deleteAll();
    userRows.deleteAll();
    passwordHash = encoder.encode(PASSWORD);
    admin = user("admin_test", RoleEnum.ROLE_ADMIN);
    customer = user("cliente_test", RoleEnum.ROLE_CLIENTE);
    other = user("otro_cliente", RoleEnum.ROLE_CLIENTE);
    seller = user("vendedor_test", RoleEnum.ROLE_VENDEDOR);
    category = inventory.category(new CategoryCommand("Papelería"));
  }

  private User user(String username, RoleEnum role) {
    var user =
        User.createNewUser(
            username,
            "Usuario Prueba",
            username + "@example.com",
            passwordHash,
            roles.findByName(role).orElseThrow());
    user.markEmailAsVerified();
    return users.save(user);
  }

  private Product product(String sku, int quantity) {
    var p =
        inventory.create(
            new ProductCommand(sku, "Cuaderno Sakura", category.id(), new BigDecimal("12.50"), 30));
    if (quantity > 0)
      inventory.move(
          new MovementCommand(p.id(), "ENTRADA", quantity, "Saldo inicial"),
          admin.getId(),
          admin.getFullName());
    return p;
  }

  private int stock(Long id) {
    return inventory.products(null, false).stream()
        .filter(p -> p.id().equals(id))
        .findFirst()
        .orElseThrow()
        .stock();
  }

  private RequestPostProcessor as(User user) {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new SessionIdentity(user.getId(), user.getUsername(), user.getPassword()),
            null,
            List.of(new SimpleGrantedAuthority(user.getRole().getName().name()))));
  }

  private SaleCommand command(Product p, int quantity) {
    return new SaleCommand(List.of(new SaleItem(p.id(), quantity)), "EN_TIENDA", null);
  }

  private LoginCommand login(User user) {
    var command = new LoginCommand();
    command.setUsername(user.getUsername());
    command.setPassword(PASSWORD);
    return command;
  }

  private String[] captureCode() {
    final String[] sent = {null};
    doAnswer(
            call -> {
              sent[0] = call.getArgument(2);
              return null;
            })
        .when(mail)
        .sendVerificationCode(anyString(), anyString(), anyString());
    return sent;
  }

  @Test
  void publicRegistrationOtpAndCheckoutUseClientRoleAndServerPrice() throws Exception {
    var p = product("REG-1", 10);
    var code = captureCode();
    var initial = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    var session = (MockHttpSession) initial.getRequest().getSession();
    String csrf = json.readTree(initial.getResponse().getContentAsString()).get("token").asText();
    mvc.perform(
            post("/api/auth/registro")
                .session(session)
                .header("X-CSRF-TOKEN", csrf)
                .contentType("application/json")
                .content(
                    """
                    {"nombre":"Cliente Nuevo","username":"nuevo_cliente","email":"nuevo@example.com",
                     "password":"SafePassword123!","roleId":1,"roleName":"ROLE_ADMIN"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresOtp").value(true));
    var saved = users.findByUsername("nuevo_cliente").orElseThrow();
    assertEquals(RoleEnum.ROLE_CLIENTE, saved.getRole().getName());
    assertFalse(saved.isEmailVerified());
    assertNotEquals(PASSWORD, saved.getPassword());
    String previousSessionId = session.getId();
    var verification =
        mvc.perform(
                post("/api/auth/verificar-email")
                    .session(session)
                    .header("X-CSRF-TOKEN", csrf)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("email", saved.getEmail(), "codigo", code[0]))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.roleName").value("ROLE_CLIENTE"))
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andReturn();
    session = (MockHttpSession) verification.getRequest().getSession();
    assertNotEquals(previousSessionId, session.getId());
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
    var refreshed =
        mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
    String newCsrf =
        json.readTree(refreshed.getResponse().getContentAsString()).get("token").asText();
    mvc.perform(
            post("/api/sales")
                .session(session)
                .header("X-CSRF-TOKEN", newCsrf)
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"productId\":"
                        + p.id()
                        + ",\"quantity\":2,\"unitPrice\":0.01}],\"paymentMethod\":\"EN_TIENDA\",\"total\":0.02}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total").value(25.0))
        .andExpect(jsonPath("$.customerId").value(saved.getId()))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    assertEquals(8, stock(p.id()));
    assertEquals(1, sales.count());
  }

  @Test
  void failedOtpAttemptsCommitAndFifthAttemptBlocksEvenTheCorrectCode() {
    var code = captureCode();
    var challenge = auth.login(login(customer));
    String wrong = "000000".equals(code[0]) ? "000001" : "000000";
    for (int i = 1; i <= 5; i++) {
      assertThrows(
          DomainException.class,
          () -> auth.verify(challenge.id(), customer.getEmail(), wrong, false, null));
      assertEquals(i, challenges.findById(challenge.id()).orElseThrow().getAttempts());
    }
    assertThrows(
        DomainException.class,
        () -> auth.verify(challenge.id(), customer.getEmail(), code[0], false, null));
  }

  @Test
  void otpCannotBeConsumedTwice() {
    var code = captureCode();
    var challenge = auth.login(login(customer));
    assertTrue(
        auth.verify(challenge.id(), customer.getEmail(), code[0], false, null).isEmailVerified());
    assertTrue(challenges.findById(challenge.id()).orElseThrow().isUsed());
    assertThrows(
        DomainException.class,
        () -> auth.verify(challenge.id(), customer.getEmail(), code[0], false, null));
  }

  @Test
  void mailFailureRollsBackBothAccountAndChallenge() {
    doThrow(new MailDeliveryException("Prueba de proveedor no disponible"))
        .when(mail)
        .sendVerificationCode(anyString(), anyString(), anyString());
    var request = new RegisterUserCommand();
    request.setNombre("Cuenta Fallida");
    request.setUsername("sin_correo");
    request.setEmail("fallo@example.com");
    request.setPassword(PASSWORD);
    assertThrows(MailDeliveryException.class, () -> auth.register(request));
    assertFalse(users.existsByUsername("sin_correo"));
    assertEquals(0, challenges.count());
  }

  @Test
  void resetChangesHashInvalidatesPendingCodesAndRejectsOldSession() throws Exception {
    var code = captureCode();
    var initial =
        new com.sakurastore.backend.infrastructure.persistence.entity.AuthChallengeJpaEntity();
    initial.setId(UUID.randomUUID());
    initial.setUserId(customer.getId());
    initial.setPurpose("LOGIN");
    initial.setCodeHash(encoder.encode("123456"));
    initial.setCreatedAt(LocalDateTime.now().minusMinutes(2));
    initial.setExpiresAt(LocalDateTime.now().plusMinutes(8));
    challenges.saveAndFlush(initial);
    var reset = auth.requestReset(customer.getEmail());
    auth.verify(reset.id(), customer.getEmail(), code[0], true, "ChangedPassword123!");
    var current = users.findById(customer.getId()).orElseThrow();
    assertTrue(encoder.matches("ChangedPassword123!", current.getPassword()));
    assertTrue(challenges.findById(initial.getId()).orElseThrow().isUsed());
    mvc.perform(get("/api/sales").with(as(customer))).andExpect(status().isUnauthorized());
  }

  @Test
  void twoConcurrentOtpVerificationsOnlyConsumeOnce() throws Exception {
    var code = captureCode();
    var challenge = auth.login(login(customer));
    var executor = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    Callable<Boolean> verify =
        () -> {
          gate.await();
          try {
            auth.verify(challenge.id(), customer.getEmail(), code[0], false, null);
            return true;
          } catch (DomainException e) {
            return false;
          }
        };
    try {
      var a = executor.submit(verify);
      var b = executor.submit(verify);
      gate.countDown();
      assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void clientCannotUseAdministrationOrInternalInventory() throws Exception {
    mvc.perform(get("/api/users").with(as(customer))).andExpect(status().isForbidden());
    mvc.perform(get("/api/roles").with(as(customer))).andExpect(status().isForbidden());
    mvc.perform(get("/api/inventory/summary").with(as(customer)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/inventory/movements")
                .with(as(customer))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/purchases")
                .with(as(customer))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/purchases")
                .with(as(seller))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void csrfIsRequiredEvenForLoggedInClient() throws Exception {
    mvc.perform(post("/api/sales").with(as(customer)).contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void catalogIsPublicPagedFilteredAndDoesNotExposeInternalStock() throws Exception {
    product("CAT-1", 4);
    product("CAT-2", 0);
    mvc.perform(get("/api/catalog/products").param("sku", "CAT-1").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].sku").value("CAT-1"))
        .andExpect(jsonPath("$.content[0].stock").doesNotExist())
        .andExpect(jsonPath("$.content[0].minStock").doesNotExist());
    mvc.perform(get("/api/catalog/products").param("available", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void columnFiltersUseIntersectionAndValidatePageAndSort() throws Exception {
    product("FILTRO-1", 5);
    product("FILTRO-2", 5);
    mvc.perform(
            get("/api/inventory/products/page")
                .with(as(admin))
                .param("sku", "FILTRO-1")
                .param("minPrice", "10")
                .param("maxPrice", "15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get("/api/users/page")
                .with(as(admin))
                .param("role", "ROLE_CLIENTE")
                .param("username", "cliente_test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].password").doesNotExist());
    mvc.perform(get("/api/catalog/products").param("size", "101"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/catalog/products").param("page", "-1"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/catalog/products").param("sort", "password,asc"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void searchTextIsLiteralAndCannotAlterTheQuery() throws Exception {
    product("SEARCH-1", 5);
    for (String text : List.of("%", "_", "' OR 1=1 --"))
      mvc.perform(get("/api/catalog/products").param("q", text))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void clientHistoryAndDetailAreRestrictedToOwnAccount() throws Exception {
    var p = product("OWNER-1", 10);
    var mine = commerce.sell(command(p, 1), customer.getId());
    var theirs = commerce.sell(command(p, 1), other.getId());
    mvc.perform(get("/api/sales").with(as(customer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(mine.id()));
    mvc.perform(get("/api/sales").with(as(customer)).param("customerId", other.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/sales/" + theirs.id()).with(as(customer)))
        .andExpect(status().isNotFound());
  }

  @Test
  void clientCannotChangeBuyerOrPretendToPayElectronically() throws Exception {
    var p = product("PAY-1", 10);
    var body = new SaleCommand(List.of(new SaleItem(p.id(), 1)), "EN_TIENDA", other.getId());
    mvc.perform(
            post("/api/sales")
                .with(as(customer))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isForbidden());
    assertThrows(
        DomainException.class,
        () -> commerce.sell(new SaleCommand(body.items(), "TARJETA", null), customer.getId()));
    assertEquals(10, stock(p.id()));
    assertEquals(0, sales.count());
  }

  @Test
  void insufficientMultiProductSaleLeavesEverythingUnchanged() {
    var a = product("ATOMIC-1", 10);
    var b = product("ATOMIC-2", 1);
    long before = movements.count();
    var request =
        new SaleCommand(
            List.of(new SaleItem(a.id(), 2), new SaleItem(b.id(), 5)), "EN_TIENDA", null);
    assertThrows(DomainException.class, () -> commerce.sell(request, customer.getId()));
    assertEquals(10, stock(a.id()));
    assertEquals(1, stock(b.id()));
    assertEquals(0, sales.count());
    assertEquals(before, movements.count());
  }

  @Test
  void duplicatedProductsAndInactiveProductsAreRejected() {
    var p = product("DUP-1", 0);
    assertThrows(
        DomainException.class,
        () ->
            commerce.sell(
                new SaleCommand(
                    List.of(new SaleItem(p.id(), 1), new SaleItem(p.id(), 1)), "EN_TIENDA", null),
                customer.getId()));
    inventory.status(p.id(), false);
    assertThrows(DomainException.class, () -> commerce.sell(command(p, 1), customer.getId()));
  }

  @Test
  void adminPurchaseAddsStockAndStoresCostAndMovement() throws Exception {
    var p = product("BUY-1", 2);
    var body =
        new PurchaseCommand(
            List.of(new PurchaseItem(p.id(), 4, new BigDecimal("6.25"))), "Factura 123");
    mvc.perform(
            post("/api/purchases")
                .with(as(admin))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total").value(25.0))
        .andExpect(jsonPath("$.items[0].unitPrice").value(6.25));
    assertEquals(6, stock(p.id()));
    assertEquals(1, purchases.count());
    assertEquals("ENTRADA", inventory.movements(p.id(), 0).get(0).type());
  }

  @Test
  void twoConcurrentSalesCannotOversellStock() throws Exception {
    var p = product("CONCURRENT-1", 10);
    var executor = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    Callable<Boolean> buy =
        () -> {
          gate.await();
          try {
            commerce.sell(command(p, 7), customer.getId());
            return true;
          } catch (DomainException e) {
            return false;
          }
        };
    try {
      var a = executor.submit(buy);
      var b = executor.submit(buy);
      gate.countDown();
      assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
      assertEquals(3, stock(p.id()));
      assertEquals(1, sales.count());
      assertEquals(2, movements.count());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void frontendSaleBodyCalculatesAmountsAndReturnsItsExactHistoryContract() throws Exception {
    var p = product("FRONT-1", 10);
    mvc.perform(post("/api/sales").with(as(seller)).with(csrf())
        .contentType("application/json").content(json.writeValueAsString(Map.of(
            "customerName", "Cliente de mostrador", "discount", 5,
            "items", List.of(Map.of("productId", p.id(), "quantity", 2))))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.subtotal").value(25.0))
        .andExpect(jsonPath("$.discount").value(5.0))
        .andExpect(jsonPath("$.total").value(20.0))
        .andExpect(jsonPath("$.customerName").value("Cliente de mostrador"))
        .andExpect(jsonPath("$.sellerName").value(seller.getFullName()))
        .andExpect(jsonPath("$.items[0].sku").value("FRONT-1"))
        .andExpect(jsonPath("$.items[0].unitPrice").value(12.5))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    mvc.perform(get("/api/sales").with(as(admin)).param("page", "0")
        .param("size", "10").param("status", "COMPLETED"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].total").value(20.0));
    assertEquals(8, stock(p.id()));
  }

  @Test
  void invalidDiscountsAndNullItemsCannotWriteSaleOrStock() throws Exception {
    var p = product("DISC-1", 10);
    long before = movements.count();
    for (String discount : List.of("-1", "100", "0.001")) {
      mvc.perform(post("/api/sales").with(as(seller)).with(csrf())
          .contentType("application/json").content("{\"discount\":" + discount
              + ",\"items\":[{\"productId\":" + p.id() + ",\"quantity\":1}]}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(post("/api/sales").with(as(seller)).with(csrf())
        .contentType("application/json").content("{\"items\":[null]}"))
        .andExpect(status().isBadRequest());
    assertEquals(0, sales.count());
    assertEquals(10, stock(p.id()));
    assertEquals(before, movements.count());
  }

  @Test
  void clientDiscountIsRejectedAndCustomerNameComesFromAuthenticatedAccount() throws Exception {
    var p = product("SELF-1", 10);
    mvc.perform(post("/api/sales").with(as(customer)).with(csrf())
        .contentType("application/json").content(json.writeValueAsString(Map.of(
            "discount", 1, "items", List.of(Map.of("productId", p.id(), "quantity", 1))))))
        .andExpect(status().isBadRequest());
    assertEquals(0, sales.count());
    mvc.perform(post("/api/sales").with(as(customer)).with(csrf())
        .contentType("application/json").content(json.writeValueAsString(Map.of(
            "customerName", "Cuenta ajena", "discount", 0,
            "items", List.of(Map.of("productId", p.id(), "quantity", 1))))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerId").value(customer.getId()))
        .andExpect(jsonPath("$.customerName").value(customer.getFullName()));
  }

  @Test
  void productAndSellerSnapshotsSurviveCatalogAndAccountEdits() throws Exception {
    var p = product("HISTORY-1", 10);
    var sale = commerce.sell(command(p, 1), seller.getId());
    var purchase = commerce.purchase(new PurchaseCommand(
        List.of(new PurchaseItem(p.id(), 1, new BigDecimal("6.25"))), "Factura original"), admin.getId());
    inventory.update(p.id(), new ProductCommand(
        "HISTORY-NEW", "Nombre cambiado", category.id(), new BigDecimal("99.00"), 30));
    var row = userRows.findById(seller.getId()).orElseThrow();
    row.setFullName("Vendedor renombrado");
    userRows.saveAndFlush(row);
    var savedSale = commerce.sale(sale.id(), admin.getId());
    assertEquals(p.name(), savedSale.items().get(0).productName());
    assertEquals("HISTORY-1", savedSale.items().get(0).sku());
    assertEquals(0, new BigDecimal("12.50").compareTo(savedSale.items().get(0).unitPrice()));
    assertEquals(seller.getFullName(), savedSale.sellerName());
    assertEquals("HISTORY-1", commerce.purchase(purchase.id(), admin.getId()).items().get(0).sku());
  }

  @Test
  void frontendProductPagingFiltersAndSummaryUseJpa() throws Exception {
    product("PAGE-A", 5);
    product("PAGE-B", 40);
    var inactive = product("PAGE-INACTIVE", 0);
    inventory.status(inactive.id(), false);
    mvc.perform(get("/api/inventory/products").with(as(customer))
        .param("page", "0").param("size", "8").param("name", "Sakura").param("status", "ACTIVE"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
    mvc.perform(get("/api/inventory/products").with(as(seller)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    mvc.perform(get("/api/inventory/summary").with(as(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.activeCount").value(2))
        .andExpect(jsonPath("$.lowCount").value(1))
        .andExpect(jsonPath("$.units").value(45))
        .andExpect(jsonPath("$.value").value(562.5));
  }

  @Test
  void frontendUsersPageCanFilterByRoleIdWithoutReturningPassword() throws Exception {
    mvc.perform(get("/api/users").with(as(admin)).param("page", "0").param("size", "10")
        .param("roleId", customer.getRole().getId().toString()).param("status", "ACTIVE")
        .param("username", "cliente_test"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].username").value("cliente_test"))
        .andExpect(jsonPath("$.content[0].password").doesNotExist());
  }

  @Test
  void persistenceFailureAfterFirstMovementRollsBackTheEntireSale() {
    var a = product("ROLL-A", 10);
    var b = product("ROLL-B", 10);
    long before = movements.count();
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    doAnswer(call -> {
      if (calls.incrementAndGet() == 2) throw new DomainException("Fallo simulado al guardar movimiento");
      return call.callRealMethod();
    }).when(inventoryRows).recordMovement(any(), any(), anyInt(), anyLong(), anyString());
    assertThrows(DomainException.class, () -> commerce.sell(new SaleCommand(
        List.of(new SaleItem(a.id(), 1), new SaleItem(b.id(), 1)), "EN_TIENDA", null), seller.getId()));
    assertEquals(2, calls.get());
    assertEquals(0, sales.count());
    assertEquals(before, movements.count());
    assertEquals(10, stock(a.id()));
    assertEquals(10, stock(b.id()));
  }

  @Test
  void reportTotalsAndRankingRespectDiscountsStatusAndHistoricalPrices() throws Exception {
    var a = product("REPORT-A", 50); var b = product("REPORT-B", 50);
    var sale = commerce.sell(new SaleCommand(List.of(new SaleItem(a.id(), 2), new SaleItem(b.id(), 1)),
        "EFECTIVO", null, "María José", new BigDecimal("2.50")), seller.getId());
    var cancelled = commerce.sell(command(a, 1), seller.getId());
    var row = sales.findById(cancelled.id()).orElseThrow(); row.setStatus("CANCELLED"); sales.saveAndFlush(row);
    mvc.perform(get("/api/reports/summary").with(as(admin))).andExpect(status().isOk())
      .andExpect(jsonPath("$.salesCount").value(1)).andExpect(jsonPath("$.subtotal").value(37.5))
      .andExpect(jsonPath("$.discount").value(2.5)).andExpect(jsonPath("$.total").value(35)).andExpect(jsonPath("$.averageTicket").value(35));
    mvc.perform(get("/api/reports/sales").with(as(seller))).andExpect(status().isOk())
      .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(sale.id()));
    inventory.update(a.id(), new ProductCommand(a.sku(), "Cuaderno nuevo", category.id(), new BigDecimal("99.00"), 30));
    mvc.perform(get("/api/reports/top-products").with(as(seller)).param("size", "1")).andExpect(status().isOk())
      .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
      .andExpect(jsonPath("$.content[0].productId").value(a.id())).andExpect(jsonPath("$.content[0].units").value(2))
      .andExpect(jsonPath("$.content[0].grossAmount").value(25));
    mvc.perform(get("/api/reports/top-products").with(as(admin)).param("page", "1").param("size", "1"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].productId").value(b.id()));
  }
  @Test
  void reportDatesIncludeEntireLastDayAndExcludeNextDay() throws Exception {
    var p = product("DATE-REPORT", 50);
    var a = commerce.sell(command(p, 1), seller.getId()); var b = commerce.sell(command(p, 1), seller.getId());
    var first = sales.findById(a.id()).orElseThrow(); first.setCreatedAt(LocalDateTime.of(2026,10,10,23,59,59)); sales.saveAndFlush(first);
    var next = sales.findById(b.id()).orElseThrow(); next.setCreatedAt(LocalDateTime.of(2026,10,11,0,0)); sales.saveAndFlush(next);
    mvc.perform(get("/api/reports/sales").with(as(admin)).param("from","2026-10-10").param("to","2026-10-10"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(a.id()));
    mvc.perform(get("/api/reports/summary").with(as(admin)).param("from","2026-10-12").param("to","2026-10-12"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.salesCount").value(0)).andExpect(jsonPath("$.total").value(0));
  }
  @Test
  void reportStockIncludesEqualityAndFiltersCategoryAndActiveProducts() throws Exception {
    var low = product("LOW",30); product("HIGH",31); var off = product("OFF",0); inventory.status(off.id(),false);
    var otherCategory=inventory.category(new CategoryCommand("Accesorios"));
    inventory.create(new ProductCommand("OTHER","Diadema",otherCategory.id(),BigDecimal.TEN,30));
    mvc.perform(get("/api/reports/stock").with(as(admin)).param("lowStock","true").param("categoryId",category.id().toString()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].productId").value(low.id())).andExpect(jsonPath("$.content[0].lowStock").value(true))
      .andExpect(jsonPath("$.content[0].retailValue").value(375));
  }
  @Test
  void reportMovementsFilterProductTypeAndDates() throws Exception {
    var p=product("MOVE",40);product("MOVE-OTHER",10);commerce.sell(command(p,2),seller.getId());
    mvc.perform(get("/api/reports/movements").with(as(seller)).param("productId",p.id().toString()).param("type","SALIDA"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].quantity").value(2)).andExpect(jsonPath("$.content[0].previousStock").value(40))
      .andExpect(jsonPath("$.content[0].newStock").value(38));
    mvc.perform(get("/api/reports/movements").with(as(admin)).param("from","2000-01-01").param("to","2000-01-01"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  }
  @Test
  void reportEndpointsRequireStaffIncludingCsv() throws Exception {
    for(String endpoint:List.of("summary","sales","top-products","stock","movements","sales/csv","low-stock/csv")) {
      mvc.perform(get("/api/reports/"+endpoint)).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/reports/"+endpoint).with(as(customer))).andExpect(status().isForbidden());
      mvc.perform(get("/api/reports/"+endpoint).with(as(seller))).andExpect(status().isOk());
    }
  }
  @Test
  void invalidReportParametersReturnControlledBadRequest() throws Exception {
    mvc.perform(get("/api/reports/sales").with(as(admin)).param("from","2026-10-11").param("to","2026-10-10")).andExpect(status().isBadRequest());
    for(String size:List.of("0","101","-1"))mvc.perform(get("/api/reports/sales").with(as(admin)).param("size",size)).andExpect(status().isBadRequest());
    mvc.perform(get("/api/reports/movements").with(as(admin)).param("type","INVALID")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/reports/movements").with(as(admin)).param("productId","-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/reports/stock").with(as(admin)).param("categoryId","-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/reports/sales").with(as(admin)).param("from","no-es-fecha")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/reports/unknown/csv").with(as(admin))).andExpect(status().isBadRequest());
  }
  @Test
  void csvExportsAllPagesWithUtf8EscapingAndFormulaProtection() throws Exception {
    var p=product("CSV",0);var rows=new ArrayList<com.sakurastore.backend.infrastructure.persistence.entity.MovementJpaEntity>();
    for(int i=0;i<105;i++) {
      var row=new com.sakurastore.backend.infrastructure.persistence.entity.MovementJpaEntity();
      row.setProduct(products.getReferenceById(p.id()));row.setActor(userRows.getReferenceById(admin.getId()));
      row.setType("ENTRADA");row.setQuantity(1);row.setPreviousStock(i);row.setNewStock(i+1);
      row.setReason(i==104?"=SUM(1;2)":"Reposición; \"Sakura\"");rows.add(row);
    }
    movements.saveAllAndFlush(rows);
    var result=mvc.perform(get("/api/reports/movements/csv").with(as(admin)).param("productId",p.id().toString()))
      .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
      .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment"))).andReturn();
    String csv=result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(csv.startsWith("\uFEFF"));assertEquals(106,csv.lines().count());
    assertTrue(csv.contains("'=SUM(1;2)"));assertTrue(csv.contains("Reposición; \"\"Sakura\"\""));
  }
  @Test
  void namesRejectNumbersSymbolsAndWhitespaceAcrossAllWriteEndpoints() throws Exception {
    var p=product("VALID",0);long productCount=products.count();long userCount=userRows.count();
    for(String name:List.of("Cliente123","María@","Cuaderno-azul","123","   ","O'Connor","Ana\nMaría")) {
      var body=new ProductCommand("NEW",name,category.id(),BigDecimal.TEN,30);
      mvc.perform(post("/api/inventory/products").with(as(admin)).with(csrf()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
      mvc.perform(put("/api/inventory/products/"+p.id()).with(as(admin)).with(csrf()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
      mvc.perform(post("/api/users").with(as(admin)).with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(new CreateUserCommand("nuevo",name,"new@example.com",PASSWORD,customer.getRole().getId())))).andExpect(status().isBadRequest());
      mvc.perform(put("/api/users/"+customer.getId()).with(as(admin)).with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(new UpdateUserCommand(name,customer.getEmail(),customer.getRole().getId())))).andExpect(status().isBadRequest());
      mvc.perform(post("/api/auth/registro").with(csrf()).contentType("application/json")
        .content(json.writeValueAsString(new RegisterUserCommand(name,"nuevo","new@example.com",PASSWORD)))).andExpect(status().isBadRequest());
      assertThrows(DomainException.class,()->inventory.create(body));
    }
    assertEquals(productCount,products.count());assertEquals(userCount,userRows.count());
    mvc.perform(post("/api/inventory/products").with(as(admin)).with(csrf()).contentType("application/json")
      .content(json.writeValueAsString(new ProductCommand("ACCENT-1","Muñeca de colección",category.id(),BigDecimal.TEN,30))))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Muñeca de colección"));
  }
  @Test
  void saleRejectsInvalidCustomerNameWithoutChangingStock() throws Exception {
    var p=product("BAD-NAME",40);
    mvc.perform(post("/api/sales").with(as(seller)).with(csrf()).contentType("application/json")
      .content(json.writeValueAsString(new SaleCommand(List.of(new SaleItem(p.id(),1)),"EFECTIVO",null,"Ana123",BigDecimal.ZERO))))
      .andExpect(status().isBadRequest());
    assertEquals(40,stock(p.id()));assertEquals(0,sales.count());
  }

}
