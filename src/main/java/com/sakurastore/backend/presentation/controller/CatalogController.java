package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.application.dto.CommerceDtos.CatalogProduct;
import com.sakurastore.backend.application.port.InventoryRepositoryPort;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
  private final InventoryRepositoryPort inventory;

  public CatalogController(InventoryRepositoryPort inventory) {
    this.inventory = inventory;
  }

  @GetMapping("/products")
  @Transactional(readOnly = true)
  public PageResponse<CatalogProduct> products(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Boolean available,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(defaultValue = "name,asc") String sort) {
    var filter =
        new ProductFilter(q, sku, name, categoryId, minPrice, maxPrice, true, false, available);
    return inventory
        .searchProducts(filter, page, size, sort)
        .map(
            p ->
                new CatalogProduct(
                    p.id(),
                    p.sku(),
                    p.name(),
                    p.categoryId(),
                    p.categoryName(),
                    p.price(),
                    p.stock() > 0));
  }
}
