package com.sakurastore.backend.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.port.InventoryRepositoryPort;
import com.sakurastore.backend.application.usecase.InventoryService;
import com.sakurastore.backend.domain.exception.DomainException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
  private final InventoryRepositoryPort repo = mock(InventoryRepositoryPort.class);
  private final InventoryService service = new InventoryService(repo);

  private Product product(int stock, boolean active) {
    return new Product(1L, "SKU", "Producto", 1L, "General", BigDecimal.TEN, 2, stock, active);
  }

  @Test
  void insufficientStockDoesNotWrite() {
    when(repo.lockProduct(1L)).thenReturn(Optional.of(product(2, true)));
    assertThrows(
        DomainException.class,
        () -> service.move(new MovementCommand(1L, "SALIDA", 3, "Venta"), 1L, "admin"));
    verify(repo, never()).setStock(any(), anyInt());
  }

  @Test
  void validMovementRecordsBeforeAndAfter() {
    var p = product(10, true);
    var c = new MovementCommand(1L, "SALIDA", 3, "Venta");
    when(repo.lockProduct(1L)).thenReturn(Optional.of(p));
    service.move(c, 4L, "alvaro");
    var order = inOrder(repo);
    order.verify(repo).lockProduct(1L);
    order.verify(repo).setStock(1L, 7);
    order.verify(repo).recordMovement(p, c, 7, 4L, "alvaro");
  }

  @Test
  void inactiveProductRejectsMovement() {
    when(repo.lockProduct(1L)).thenReturn(Optional.of(product(2, false)));
    assertThrows(
        DomainException.class,
        () -> service.move(new MovementCommand(1L, "ENTRADA", 3, "Compra"), 1L, "admin"));
    verify(repo, never()).setStock(any(), anyInt());
  }

  @Test
  void cannotDeactivateStock() {
    when(repo.lockProduct(1L)).thenReturn(Optional.of(product(2, true)));
    assertThrows(DomainException.class, () -> service.status(1L, false));
    verify(repo, never()).setActive(any(), anyBoolean());
  }
}
