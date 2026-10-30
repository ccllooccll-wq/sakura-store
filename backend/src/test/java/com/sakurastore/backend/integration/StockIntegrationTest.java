package com.sakurastore.backend.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.sakurastore.backend.application.dto.CommerceDtos.*;
import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.usecase.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.*;
import com.sakurastore.backend.infrastructure.persistence.adapter.JpaInventoryRepository;
import com.sakurastore.backend.infrastructure.persistence.repository.*;
import com.sakurastore.backend.infrastructure.security.SessionIdentity;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.IllegalTransactionStateException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockIntegrationTest {
  @Autowired InventoryService inventory;
  @Autowired CommerceService commerce;
  @Autowired StockIntegrationService integration;
  @Autowired UserRepositoryPort users;
  @Autowired RoleRepositoryPort roles;
  @Autowired PasswordEncoder encoder;
  @Autowired SpringDataUserRepository userRows;
  @Autowired AuthChallengeJpaRepository challenges;
  @Autowired EmailVerificationJpaRepository legacyCodes;
  @Autowired SaleJpaRepository sales;
  @Autowired PurchaseJpaRepository purchases;
  @Autowired MovementJpaRepository movements;
  @Autowired ProductJpaRepository products;
  @Autowired CategoryJpaRepository categories;
  @Autowired MockMvc mvc;
  @MockBean EmailSenderPort mail;
  @SpyBean JpaInventoryRepository inventoryRows;
  private User admin, customer;
  private Category category;

  @BeforeEach void prepare() {
    sales.deleteAll(); purchases.deleteAll(); movements.deleteAll(); products.deleteAll();
    categories.deleteAll(); challenges.deleteAll(); legacyCodes.deleteAll(); userRows.deleteAll();
    String hash = encoder.encode("TestPassword123!");
    admin = user("derik_admin", RoleEnum.ROLE_ADMIN, hash);
    customer = user("derik_cliente", RoleEnum.ROLE_CLIENTE, hash);
    category = inventory.category(new CategoryCommand("Inventario Sprint 3"));
  }
  private User user(String name, RoleEnum role, String hash) {
    var u = User.createNewUser(name, "Usuario Prueba", name + "@example.com", hash,
        roles.findByName(role).orElseThrow());
    u.markEmailAsVerified(); return users.save(u);
  }
  private Product product(String sku, int stock) {
    var p = inventory.create(new ProductCommand(sku, "Producto Sakura", category.id(),
        new BigDecimal("12.50"), 30));
    if (stock > 0) inventory.move(new MovementCommand(p.id(), "ENTRADA", stock, "Saldo inicial"),
        admin.getId(), admin.getFullName());
    return p;
  }
  private int stock(Long id) {
    return inventory.products("", false).stream().filter(p -> p.id().equals(id))
        .findFirst().orElseThrow().stock();
  }
  private SaleCommand sale(Product p, int q) {
    return new SaleCommand(List.of(new SaleItem(p.id(), q)), "EN_TIENDA", null);
  }
  private PurchaseCommand purchase(Product p, int q) {
    return new PurchaseCommand(List.of(new PurchaseItem(p.id(), q, new BigDecimal("6.25"))), "Factura");
  }
  @Test void purchaseAddsStockOnceAndCreatesOneEntry() {
    var p = product("ENTRADA-1", 0);
    var saved = commerce.purchase(purchase(p, 8), admin.getId());
    assertEquals(8, stock(p.id())); assertEquals(1, purchases.count()); assertEquals(1, movements.count());
    var m = inventory.movements(p.id(), 0).get(0);
    assertEquals("ENTRADA", m.type()); assertEquals(0, m.previousStock());
    assertEquals(8, m.newStock()); assertEquals(8, m.quantity());
    assertEquals("Compra #" + saved.id(), m.reason());
  }
  @Test void saleReducesStockOnceAndCreatesOneExit() {
    var p = product("SALIDA-1", 15); long before = movements.count();
    var saved = commerce.sell(sale(p, 4), customer.getId());
    assertEquals(11, stock(p.id())); assertEquals(before + 1, movements.count());
    var m = inventory.movements(p.id(), 0).get(0);
    assertEquals("SALIDA", m.type()); assertEquals(15, m.previousStock());
    assertEquals(11, m.newStock()); assertEquals(4, m.quantity());
    assertEquals("Venta #" + saved.id(), m.reason());
  }
  @Test void multiProductPurchaseUpdatesEveryProductOnce() {
    var a = product("MULTI-A", 4); var b = product("MULTI-B", 6); long before = movements.count();
    commerce.purchase(new PurchaseCommand(List.of(new PurchaseItem(b.id(), 8, BigDecimal.ONE),
        new PurchaseItem(a.id(), 5, BigDecimal.ONE)), "Factura"), admin.getId());
    assertEquals(9, stock(a.id())); assertEquals(14, stock(b.id()));
    assertEquals(before + 2, movements.count());
  }
  @Test void insufficientSalePreservesAllStocksAndMovements() {
    var a = product("INSUF-A", 7); var b = product("INSUF-B", 1); long before = movements.count();
    assertThrows(DomainException.class, () -> commerce.sell(new SaleCommand(List.of(
        new SaleItem(a.id(), 2), new SaleItem(b.id(), 3)), "EN_TIENDA", null), customer.getId()));
    assertEquals(7, stock(a.id())); assertEquals(1, stock(b.id()));
    assertEquals(0, sales.count()); assertEquals(before, movements.count());
  }
  private AtomicInteger failSecondMovement() {
    var calls = new AtomicInteger();
    doAnswer(call -> {
      if (calls.incrementAndGet() == 2) throw new DomainException("Fallo simulado.");
      return call.callRealMethod();
    }).when(inventoryRows).recordMovement(any(), any(), anyInt(), anyLong(), anyString());
    return calls;
  }
  @Test void failedPurchaseRollsBackHeaderDetailsAndAllStocks() {
    var a = product("ROLL-COMPRA-A", 4); var b = product("ROLL-COMPRA-B", 6);
    long before = movements.count(); var calls = failSecondMovement();
    assertThrows(DomainException.class, () -> commerce.purchase(new PurchaseCommand(List.of(
        new PurchaseItem(a.id(), 5, BigDecimal.ONE), new PurchaseItem(b.id(), 8, BigDecimal.ONE)),
        "Factura fallida"), admin.getId()));
    assertEquals(2, calls.get()); assertEquals(0, purchases.count());
    assertEquals(4, stock(a.id())); assertEquals(6, stock(b.id()));
    assertEquals(before, movements.count());
  }
  @Test void failedSaleRollsBackHeaderDetailsAndAllStocks() {
    var a = product("ROLL-VENTA-A", 7); var b = product("ROLL-VENTA-B", 8);
    long before = movements.count(); var calls = failSecondMovement();
    assertThrows(DomainException.class, () -> commerce.sell(new SaleCommand(List.of(
        new SaleItem(a.id(), 2), new SaleItem(b.id(), 3)), "EN_TIENDA", null), customer.getId()));
    assertEquals(2, calls.get()); assertEquals(0, sales.count());
    assertEquals(7, stock(a.id())); assertEquals(8, stock(b.id()));
    assertEquals(before, movements.count());
  }
  @Test void concurrentSalesCannotOversellTheSameStock() throws Exception {
    var p = product("CONCURRENT-STOCK", 10);
    var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
    Callable<Boolean> sell = () -> {
      start.await();
      try { commerce.sell(sale(p, 7), customer.getId()); return true; }
      catch (DomainException e) { return false; }
    };
    try {
      var a = executor.submit(sell); var b = executor.submit(sell); start.countDown();
      assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
      assertEquals(3, stock(p.id())); assertEquals(1, sales.count()); assertEquals(2, movements.count());
    } finally { executor.shutdownNow(); }
  }
  @Test void stockIntegrationCannotRunOutsideTheCommerceTransaction() {
    var p = product("MANDATORY-TX", 0);
    var line = new Line(p.id(), p.name(), p.sku(), 1, p.price(), p.price());
    assertThrows(IllegalTransactionStateException.class, () -> integration.registerPurchase(
        999L, List.of(line), admin.getId(), admin.getFullName()));
    assertEquals(0, stock(p.id())); assertEquals(0, movements.count());
  }
  @Test void clientCanBuyButCannotCreateManualStockMovements() throws Exception {
    var p = product("CLIENT-STOCK", 5);
    var identity = new UsernamePasswordAuthenticationToken(
        new SessionIdentity(customer.getId(), customer.getUsername(), customer.getPassword()),
        null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    mvc.perform(post("/api/inventory/movements").with(authentication(identity)).with(csrf())
        .contentType("application/json").content("{\"productId\":" + p.id()
            + ",\"type\":\"ENTRADA\",\"quantity\":20,\"reason\":\"Intento manual\"}"))
        .andExpect(status().isForbidden());
    assertEquals(5, stock(p.id()));
    mvc.perform(post("/api/sales").with(authentication(identity)).with(csrf())
        .contentType("application/json").content("{\"items\":[{\"productId\":" + p.id()
            + ",\"quantity\":1}]}")).andExpect(status().isCreated());
    assertEquals(4, stock(p.id())); assertEquals(2, movements.count());
  }
}
