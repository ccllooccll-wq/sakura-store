package com.sakurastore.backend.application.dto;

import java.math.BigDecimal;

public record ProductFilter(
    String q,
    String sku,
    String name,
    Long categoryId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Boolean active,
    boolean lowStock,
    Boolean available) {}
