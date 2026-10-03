package com.sakurastore.backend.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class InventoryDtos {
    private InventoryDtos() {}

    public record Category(Long id, String name) {}

    public record CategoryCommand(@NotBlank @Size(max = 100) String name) {}

    public record Product(
            Long id,
            String sku,
            String name,
            Long categoryId,
            String categoryName,
            BigDecimal price,
            int minStock,
            int stock,
            boolean active) {}

    public record ProductCommand(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String sku,
            @NotBlank @Size(max = 150) String name,
            @NotNull @Positive Long categoryId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @Min(30) int minStock) {}

    public record MovementCommand(
            @NotNull @Positive Long productId,
            @NotBlank @Pattern(regexp = "ENTRADA|SALIDA") String type,
            @Min(1) int quantity,
            @NotBlank @Size(max = 250) String reason) {}

    public record Movement(
            Long id,
            Long productId,
            String productName,
            String type,
            int quantity,
            int previousStock,
            int newStock,
            String reason,
            String actorName,
            LocalDateTime createdAt) {}

    public record StatusCommand(@NotNull Boolean active) {}
}