package com.sakurastore.backend.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class CommerceDtos {
  private CommerceDtos() {}

  public record SaleItem(@NotNull @Positive Long productId, @Min(1) @Max(1000000) int quantity) {}

  public record SaleCommand(
      @NotEmpty @Size(max = 100) List<@NotNull @Valid SaleItem> items,
      @Pattern(regexp = "EFECTIVO|TARJETA|QR|TRANSFERENCIA|EN_TIENDA") String paymentMethod,
      @Positive Long customerId,
      @Size(max = 200) String customerName,
      @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discount) {
    public SaleCommand {
      paymentMethod = paymentMethod == null || paymentMethod.isBlank() ? "EN_TIENDA" : paymentMethod;
      discount = discount == null ? BigDecimal.ZERO : discount;
    }

    public SaleCommand(List<SaleItem> items, String paymentMethod, Long customerId) {
      this(items, paymentMethod, customerId, null, BigDecimal.ZERO);
    }
  }

  public record PurchaseItem(
      @NotNull @Positive Long productId,
      @Min(1) @Max(1000000) int quantity,
      @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {}

  public record PurchaseCommand(
      @NotEmpty @Size(max = 100) List<@NotNull @Valid PurchaseItem> items,
      @Size(max = 200) String supplierReference) {}

  public record Line(
      Long productId, String productName, String sku, int quantity,
      BigDecimal unitPrice, BigDecimal subtotal) {}

  public record Sale(
      Long id, LocalDateTime createdAt, BigDecimal total, Long actorId,
      String actorName, Long customerId, String paymentMethod, String status,
      List<Line> items, String customerName, String sellerName,
      BigDecimal subtotal, BigDecimal discount) {}

  public record Purchase(
      Long id, LocalDateTime createdAt, BigDecimal total, Long actorId,
      String actorName, String supplierReference, List<Line> items) {}

  public record Filter(
      Long id, Long actorId, Long customerId, String paymentMethod, String status,
      LocalDate from, LocalDate to, BigDecimal minTotal, BigDecimal maxTotal) {}

  public record CatalogProduct(
      Long id, String sku, String name, Long categoryId, String categoryName,
      BigDecimal price, boolean available) {}
}
