package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.dto.ProductFilter;
import com.sakurastore.backend.application.port.InventoryRepositoryPort;
import com.sakurastore.backend.application.usecase.InventoryService;
import com.sakurastore.backend.infrastructure.security.SessionIdentity;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
  private final InventoryService inventory;
  private final InventoryRepositoryPort repository;

  public InventoryController(InventoryService inventory, InventoryRepositoryPort repository) {
    this.inventory = inventory;
    this.repository = repository;
  }

  @GetMapping("/categories")
  public List<Category> categories() {
    return inventory.categories();
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public Category category(@Valid @RequestBody CategoryCommand c) {
    return inventory.category(c);
  }

  @GetMapping(value = "/products", params = "!page")
  public List<Product> products(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "false") boolean lowStock) {
    return inventory.products(q, lowStock);
  }

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public Product create(@Valid @RequestBody ProductCommand c) {
    return inventory.create(c);
  }

  @GetMapping({"/products/page", "/products"})
  public PageResponse<Product> search(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "false") boolean lowStock,
      @RequestParam(required = false) Boolean available,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,desc") String sort) {
    if (status != null && !status.isBlank()) {
      active = switch (status) {
        case "ACTIVE" -> true;
        case "INACTIVE" -> false;
        default -> throw new com.sakurastore.backend.domain.exception.DomainException("Estado de producto inválido.");
      };
    }
    return repository.searchProducts(
        new ProductFilter(
            q, sku, name, categoryId, minPrice, maxPrice, active, lowStock, available),
        page,
        size,
        sort);
  }

  @GetMapping("/summary")
  public InventorySummary summary() { return repository.summary(); }

  @PutMapping("/products/{id}")
  public Product update(@PathVariable Long id, @Valid @RequestBody ProductCommand c) {
    return inventory.update(id, c);
  }

  @PatchMapping("/products/{id}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void status(@PathVariable Long id, @Valid @RequestBody StatusCommand c) {
    inventory.status(id, c.active());
  }

  @GetMapping("/movements")
  public List<Movement> movements(
      @RequestParam(required = false) Long productId, @RequestParam(defaultValue = "0") int page) {
    return inventory.movements(productId, page);
  }

  @PostMapping("/movements")
  @ResponseStatus(HttpStatus.CREATED)
  public Movement move(@Valid @RequestBody MovementCommand c, Authentication auth) {
    var identity = (SessionIdentity) auth.getPrincipal();
    return inventory.move(c, identity.id(), identity.username());
  }
}
