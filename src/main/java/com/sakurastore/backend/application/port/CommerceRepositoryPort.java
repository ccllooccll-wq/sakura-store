package com.sakurastore.backend.application.port;

import com.sakurastore.backend.application.dto.CommerceDtos.*;
import com.sakurastore.backend.application.dto.PageResponse;
import java.math.BigDecimal;
import java.util.*;

public interface CommerceRepositoryPort {
  Sale saveSale(
      Long actorId,
      Long customerId,
      String paymentMethod,
      String status,
      BigDecimal total,
      BigDecimal subtotal,
      BigDecimal discount,
      String customerName,
      List<Line> items);

  Purchase savePurchase(Long actorId, String supplierReference, BigDecimal total, List<Line> items);

  Optional<Sale> findSale(Long id);

  Optional<Purchase> findPurchase(Long id);

  PageResponse<Sale> sales(Filter filter, Long ownerId, int page, int size, String sort);

  PageResponse<Purchase> purchases(Filter filter, int page, int size, String sort);
}
