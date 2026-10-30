package com.sakurastore.backend.infrastructure.persistence.adapter;
import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.application.dto.ReportDtos.*;
import com.sakurastore.backend.application.port.ReportRepositoryPort;
import com.sakurastore.backend.infrastructure.persistence.entity.*;
import com.sakurastore.backend.infrastructure.persistence.repository.*;
import jakarta.persistence.criteria.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@Transactional(readOnly = true)
public class JpaReportRepository implements ReportRepositoryPort {
  private final ReportJpaRepository reports;
  private final SaleJpaRepository sales;
  private final ProductJpaRepository products;
  private final MovementJpaRepository movements;
  public JpaReportRepository(ReportJpaRepository reports, SaleJpaRepository sales, ProductJpaRepository products, MovementJpaRepository movements) {
    this.reports = reports; this.sales = sales; this.products = products; this.movements = movements;
  }
  private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
  public SalesSummary summary(LocalDateTime from, LocalDateTime until) {
    var t = reports.summary(from, until); var total = amount(t.getTotal());
    return new SalesSummary(t.getSalesCount(), amount(t.getSubtotal()), amount(t.getDiscount()), total,
        t.getSalesCount() == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(t.getSalesCount()), 2, RoundingMode.HALF_UP));
  }
  public PageResponse<SaleRow> sales(LocalDateTime from, LocalDateTime until, int page, int size) {
    Specification<SaleJpaEntity> filter = (r, q, cb) -> cb.and(cb.equal(r.get("status"), "COMPLETED"),
        cb.greaterThanOrEqualTo(r.get("createdAt"), from), cb.lessThan(r.get("createdAt"), until));
    return PageResponse.from(sales.findAll(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
        s -> new SaleRow(s.getId(), s.getCreatedAt(), s.getCustomerName(), s.getSellerName(), s.getPaymentMethod(), s.getSubtotal(), s.getDiscount(), s.getTotal()));
  }
  public PageResponse<TopProduct> topProducts(LocalDateTime from, LocalDateTime until, int page, int size) {
    return PageResponse.from(reports.topProducts(from, until, PageRequest.of(page, size)),
        p -> new TopProduct(p.getProductId(), p.getSku(), p.getName(), p.getUnits(), p.getGrossAmount()));
  }
  public PageResponse<StockRow> stock(Long categoryId, boolean lowStock, int page, int size) {
    var filter = ProductSpecifications.filter(new ProductFilter(null, null, null, categoryId, null, null, true, lowStock, null));
    return PageResponse.from(products.findAll(filter, PageRequest.of(page, size, Sort.by("name", "id"))), p -> {
      int units = p.getStock() == null ? 0 : p.getStock().getQuantity();
      int minimum = p.getStock() == null ? 30 : p.getStock().getMinStock();
      return new StockRow(p.getId(), p.getSku(), p.getName(), p.getCategory().getName(), units, minimum,
          p.getPrice(), p.getPrice().multiply(BigDecimal.valueOf(units)), units <= minimum);
    });
  }
  public PageResponse<MovementRow> movements(LocalDateTime from, LocalDateTime until, Long productId, String type, int page, int size) {
    Specification<MovementJpaEntity> filter = (r, q, cb) -> {
      if (q.getResultType() != Long.class && q.getResultType() != long.class) { r.fetch("product"); r.fetch("actor", JoinType.LEFT); }
      var predicates = new ArrayList<Predicate>();
      predicates.add(cb.greaterThanOrEqualTo(r.get("createdAt"), from)); predicates.add(cb.lessThan(r.get("createdAt"), until));
      if (productId != null) predicates.add(cb.equal(r.get("product").get("id"), productId));
      if (type != null && !type.isBlank()) predicates.add(cb.equal(r.get("type"), type));
      return cb.and(predicates.toArray(Predicate[]::new));
    };
    return PageResponse.from(movements.findAll(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
        m -> new MovementRow(m.getId(), m.getCreatedAt(), m.getProduct().getName(), m.getType(), m.getQuantity(), m.getPreviousStock(),
          m.getNewStock(), m.getReason(), m.getActor() == null ? "Sistema" : m.getActor().getFullName()));
  }
}
