package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.CommerceDtos.*;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.usecase.CommerceService;
import com.sakurastore.backend.infrastructure.security.SessionIdentity;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommerceController {
  private final CommerceService commerce;

  public CommerceController(CommerceService commerce) {
    this.commerce = commerce;
  }

  @PostMapping("/sales")
  @ResponseStatus(HttpStatus.CREATED)
  public Sale sell(@Valid @RequestBody SaleCommand command, Authentication auth) {
    return commerce.sell(command, id(auth));
  }

  @GetMapping("/sales/{id}")
  public Sale sale(@PathVariable Long id, Authentication auth) {
    return commerce.sale(id, id(auth));
  }

  @GetMapping("/sales")
  public PageResponse<Sale> sales(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) Long actorId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) String paymentMethod,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) BigDecimal minTotal,
      @RequestParam(required = false) BigDecimal maxTotal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,desc") String sort,
      Authentication auth) {
    return commerce.sales(
        new Filter(id, actorId, customerId, paymentMethod, status, from, to, minTotal, maxTotal),
        id(auth),
        page,
        size,
        sort);
  }

  @PostMapping("/purchases")
  @ResponseStatus(HttpStatus.CREATED)
  public Purchase purchase(@Valid @RequestBody PurchaseCommand command, Authentication auth) {
    return commerce.purchase(command, id(auth));
  }

  @GetMapping("/purchases/{id}")
  public Purchase purchase(@PathVariable Long id, Authentication auth) {
    return commerce.purchase(id, id(auth));
  }

  @GetMapping("/purchases")
  public PageResponse<Purchase> purchases(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) Long actorId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) BigDecimal minTotal,
      @RequestParam(required = false) BigDecimal maxTotal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,desc") String sort,
      Authentication auth) {
    return commerce.purchases(
        new Filter(id, actorId, null, null, null, from, to, minTotal, maxTotal),
        id(auth),
        page,
        size,
        sort);
  }

  private Long id(Authentication auth) {
    return ((SessionIdentity) auth.getPrincipal()).id();
  }
}
