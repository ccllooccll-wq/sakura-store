package com.sakurastore.backend.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.sakurastore.backend.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

class StockRulesTest {
  @Test
  void entrada() {
    assertEquals(12, StockRules.move(7, 5, "ENTRADA"));
  }

  @Test
  void salida() {
    assertEquals(0, StockRules.move(7, 7, "SALIDA"));
  }

  @Test
  void rejectsNegativeStock() {
    assertThrows(DomainException.class, () -> StockRules.move(2, 3, "SALIDA"));
  }

  @Test
  void rejectsInvalidQuantity() {
    assertThrows(DomainException.class, () -> StockRules.move(2, 0, "ENTRADA"));
  }

  @Test
  void rejectsOverflow() {
    assertThrows(DomainException.class, () -> StockRules.move(Integer.MAX_VALUE, 1, "ENTRADA"));
  }

  @Test
  void rejectsUnknownType() {
    assertThrows(DomainException.class, () -> StockRules.move(2, 1, "AJUSTE"));
  }
}
