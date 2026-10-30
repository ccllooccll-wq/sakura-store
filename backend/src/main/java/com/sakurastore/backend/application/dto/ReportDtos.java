package com.sakurastore.backend.application.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public final class ReportDtos {
  private ReportDtos() {}
  public record SalesSummary(long salesCount, BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal averageTicket) {}
  public record SaleRow(Long id, LocalDateTime createdAt, String customerName, String sellerName, String paymentMethod, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}
  public record TopProduct(Long productId, String sku, String name, long units, BigDecimal grossAmount) {}
  public record StockRow(Long productId, String sku, String name, String category, int quantity, int minimum, BigDecimal price, BigDecimal retailValue, boolean lowStock) {}
  public record MovementRow(Long id, LocalDateTime createdAt, String productName, String type, int quantity, Integer previousStock, Integer newStock, String reason, String actorName) {}
}
