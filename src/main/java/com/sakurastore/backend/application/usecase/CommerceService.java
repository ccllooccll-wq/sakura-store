package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.CommerceDtos.*;
import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.port.*;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.*;
import com.sakurastore.backend.domain.port.UserRepositoryPort;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CommerceService {

  private final CommerceRepositoryPort commerce;
  private final InventoryRepositoryPort inventory;
  private final UserRepositoryPort users;
  private final StockIntegrationService stockIntegration;

  private static final BigDecimal MAX_TOTAL =
          new BigDecimal("9999999999.99");

  public CommerceService(
          CommerceRepositoryPort commerce,
          InventoryRepositoryPort inventory,
          UserRepositoryPort users,
          StockIntegrationService stockIntegration) {

    this.commerce = commerce;
    this.inventory = inventory;
    this.users = users;
    this.stockIntegration = stockIntegration;
  }

  public Sale sell(SaleCommand command, Long actorId) {
    User actor = actor(actorId);

    boolean customer =
            actor.getRole().getName() == RoleEnum.ROLE_CLIENTE;

    if (!customer
            && actor.getRole().getName() != RoleEnum.ROLE_ADMIN
            && actor.getRole().getName() != RoleEnum.ROLE_VENDEDOR) {

      throw new AccessDeniedException(
              "No tienes permiso para registrar ventas.");
    }

    Long customerId = command.customerId();

    String customerName = command.customerName() == null
            ? null
            : command.customerName().trim();

    BigDecimal discount = command.discount();

    if (discount == null
            || discount.signum() < 0
            || discount.scale() > 2
            || discount.compareTo(MAX_TOTAL) > 0) {

      throw new DomainException(
              "El descuento debe ser positivo o cero y tener como máximo dos decimales.");
    }

    if (customer) {
      if (customerId != null && !customerId.equals(actorId)) {
        throw new AccessDeniedException(
                "La compra debe pertenecer a tu cuenta.");
      }

      if (!"EN_TIENDA".equals(command.paymentMethod())) {
        throw new DomainException(
                "El cliente confirma su compra con pago y retiro en tienda.");
      }

      customerId = actorId;
      customerName = actor.getFullName();

      if (discount.signum() != 0) {
        throw new DomainException(
                "Los descuentos solo pueden aplicarlos el administrador o el vendedor.");
      }

    } else if (customerId != null) {
      var account = actor(customerId);

      if (account.getRole().getName() != RoleEnum.ROLE_CLIENTE) {
        throw new DomainException(
                "La cuenta indicada no es un cliente.");
      }

      customerName = account.getFullName();
    }

    if (!Set.of(
            "EFECTIVO",
            "TARJETA",
            "QR",
            "TRANSFERENCIA",
            "EN_TIENDA").contains(command.paymentMethod())) {

      throw new DomainException("Método de pago inválido.");
    }

    if (command.items() == null
            || command.items().isEmpty()
            || command.items().size() > 100) {

      throw new DomainException(
              "La venta debe tener entre 1 y 100 productos.");
    }

    Map<Long, Integer> quantities = new TreeMap<>();

    for (var item : command.items()) {
      validItem(item.productId(), item.quantity());

      if (quantities.putIfAbsent(
              item.productId(), item.quantity()) != null) {

        throw new DomainException(
                "Cada producto debe aparecer una sola vez en la venta.");
      }
    }

    var lines = new ArrayList<Line>();
    BigDecimal total = BigDecimal.ZERO;

    for (var entry : quantities.entrySet()) {
      var p = product(entry.getKey());

      if (p.stock() < entry.getValue()) {
        throw new DomainException(
                "Stock insuficiente para " + p.name() + ".");
      }

      var subtotal = p.price()
              .multiply(BigDecimal.valueOf(entry.getValue()));

      total = total.add(subtotal);

      lines.add(new Line(
              p.id(),
              p.name(),
              p.sku(),
              entry.getValue(),
              p.price(),
              subtotal));
    }

    validTotal(total);

    BigDecimal subtotal = total;

    if (discount.compareTo(subtotal) > 0) {
      throw new DomainException(
              "El descuento no puede ser mayor al subtotal.");
    }

    total = subtotal.subtract(discount);

    var sale = commerce.saveSale(
            actorId,
            customerId,
            command.paymentMethod(),
            "COMPLETED",
            total,
            subtotal,
            discount,
            customerName,
            lines);

    stockIntegration.registerSale(
            sale.id(), lines, actorId, actor.getFullName());

    return sale;
  }

  public Purchase purchase(PurchaseCommand command, Long actorId) {
    var actor = admin(actorId);

    if (command.items() == null
            || command.items().isEmpty()
            || command.items().size() > 100) {

      throw new DomainException(
              "La compra debe tener entre 1 y 100 productos.");
    }

    Map<Long, PurchaseItem> items = new TreeMap<>();

    for (var item : command.items()) {
      validItem(item.productId(), item.quantity());

      if (item.unitPrice() == null
              || item.unitPrice().signum() < 0
              || item.unitPrice().scale() > 2) {

        throw new DomainException(
                "El costo debe ser positivo o cero y tener como máximo dos decimales.");
      }

      if (items.putIfAbsent(item.productId(), item) != null) {
        throw new DomainException("Producto repetido en la compra.");
      }
    }

    var lines = new ArrayList<Line>();
    BigDecimal total = BigDecimal.ZERO;

    for (var item : items.values()) {
      var p = product(item.productId());

      StockRules.move(p.stock(), item.quantity(), "ENTRADA");

      var subtotal = item.unitPrice()
              .multiply(BigDecimal.valueOf(item.quantity()));

      total = total.add(subtotal);

      lines.add(new Line(
              p.id(),
              p.name(),
              p.sku(),
              item.quantity(),
              item.unitPrice(),
              subtotal));
    }

    validTotal(total);

    String supplier = command.supplierReference() == null
            ? null
            : command.supplierReference().trim();

    var purchase = commerce.savePurchase(
            actorId, supplier, total, lines);

    stockIntegration.registerPurchase(
            purchase.id(), lines, actorId, actor.getFullName());

    return purchase;
  }

  @Transactional(readOnly = true)
  public Sale sale(Long id, Long actorId) {
    var actor = actor(actorId);

    var sale = commerce.findSale(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Venta no encontrada."));

    if (actor.getRole().getName() == RoleEnum.ROLE_CLIENTE
            && !actorId.equals(sale.customerId())) {

      throw new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Venta no encontrada.");
    }

    return sale;
  }

  @Transactional(readOnly = true)
  public PageResponse<Sale> sales(
          Filter filter,
          Long actorId,
          int page,
          int size,
          String sort) {

    var actor = actor(actorId);

    if (filter.status() != null
            && !Set.of("COMPLETED", "CANCELLED")
            .contains(filter.status())) {

      throw new DomainException("Estado de venta inválido.");
    }

    return commerce.sales(
            filter,
            actor.getRole().getName() == RoleEnum.ROLE_CLIENTE
                    ? actorId
                    : null,
            page,
            size,
            sort);
  }

  @Transactional(readOnly = true)
  public Purchase purchase(Long id, Long actorId) {
    admin(actorId);

    return commerce.findPurchase(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Compra no encontrada."));
  }

  @Transactional(readOnly = true)
  public PageResponse<Purchase> purchases(
          Filter filter,
          Long actorId,
          int page,
          int size,
          String sort) {

    admin(actorId);
    return commerce.purchases(filter, page, size, sort);
  }

  private User actor(Long id) {
    var user = users.findById(id)
            .orElseThrow(() -> new AccessDeniedException(
                    "La cuenta no está disponible."));

    if (!user.isActive() || !user.isEmailVerified()) {
      throw new AccessDeniedException(
              "Verifica tu cuenta antes de continuar.");
    }

    return user;
  }

  private User admin(Long id) {
    var user = actor(id);

    if (user.getRole().getName() != RoleEnum.ROLE_ADMIN) {
      throw new AccessDeniedException(
              "Solo el administrador registra compras de mercadería.");
    }

    return user;
  }

  private Product product(Long id) {
    var p = inventory.lockProduct(id)
            .orElseThrow(() -> new DomainException(
                    "Producto no encontrado: " + id));

    if (!p.active()) {
      throw new DomainException(
              "El producto está desactivado: " + p.name());
    }

    return p;
  }

  private void validItem(Long id, int quantity) {
    if (id == null || id <= 0
            || quantity <= 0 || quantity > 1000000) {

      throw new DomainException("Producto o cantidad inválidos.");
    }
  }

  private void validTotal(BigDecimal total) {
    if (total.signum() < 0 || total.compareTo(MAX_TOTAL) > 0) {
      throw new DomainException(
              "El total supera el límite permitido.");
    }
  }
}