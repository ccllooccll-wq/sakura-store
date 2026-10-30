package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.CommerceDtos.Line;
import com.sakurastore.backend.application.dto.InventoryDtos.MovementCommand;
import com.sakurastore.backend.domain.exception.DomainException;
import java.util.List;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class StockIntegrationService {
  private final InventoryService inventory;

  public StockIntegrationService(InventoryService inventory) {
    this.inventory = inventory;
  }

  public void registerSale(Long saleId, List<Line> lines, Long actorId, String actorName) {
    register("SALIDA", "Venta #", saleId, lines, actorId, actorName);
  }

  public void registerPurchase(Long purchaseId, List<Line> lines, Long actorId, String actorName) {
    register("ENTRADA", "Compra #", purchaseId, lines, actorId, actorName);
  }

  private void register(String type, String prefix, Long documentId, List<Line> lines,
      Long actorId, String actorName) {
    if (documentId == null || documentId <= 0)
      throw new DomainException("La operación debe estar guardada antes de actualizar stock.");
    if (actorId == null || actorId <= 0 || actorName == null || actorName.isBlank())
      throw new DomainException("Se requiere un usuario responsable del movimiento.");
    if (lines == null || lines.isEmpty() || lines.size() > 100)
      throw new DomainException("La operación debe tener entre 1 y 100 productos.");
    var quantities = new TreeMap<Long, Integer>();
    for (var line : lines) {
      if (line == null || line.productId() == null || line.productId() <= 0
          || line.quantity() <= 0 || line.quantity() > 1000000)
        throw new DomainException("Producto o cantidad inválidos.");
      if (quantities.putIfAbsent(line.productId(), line.quantity()) != null)
        throw new DomainException("Cada producto debe aparecer una sola vez.");
    }
    quantities.forEach((productId, quantity) -> inventory.move(
        new MovementCommand(productId, type, quantity, prefix + documentId), actorId, actorName));
  }
}
