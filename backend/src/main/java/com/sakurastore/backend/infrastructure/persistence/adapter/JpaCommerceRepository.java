package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.application.dto.CommerceDtos.*;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.port.CommerceRepositoryPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.infrastructure.persistence.entity.*;
import com.sakurastore.backend.infrastructure.persistence.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaCommerceRepository implements CommerceRepositoryPort {
  private final SaleJpaRepository sales;
  private final PurchaseJpaRepository purchases;
  private final SpringDataUserRepository users;
  private final ProductJpaRepository products;

  public JpaCommerceRepository(
      SaleJpaRepository sales,
      PurchaseJpaRepository purchases,
      SpringDataUserRepository users,
      ProductJpaRepository products) {
    this.sales = sales;
    this.purchases = purchases;
    this.users = users;
    this.products = products;
  }

  @Override
  public Sale saveSale(
      Long actorId,
      Long customerId,
      String method,
      String status,
      BigDecimal total,
      BigDecimal subtotal,
      BigDecimal discount,
      String customerName,
      List<Line> items) {
    var sale = new SaleJpaEntity();
    sale.setActor(users.getReferenceById(actorId));
    if (customerId != null) sale.setCustomer(users.getReferenceById(customerId));
    sale.setPaymentMethod(method);
    sale.setStatus(status);
    sale.setTotal(total);
    sale.setSubtotal(subtotal);
    sale.setDiscount(discount);
    sale.setCustomerName(customerName);
    sale.setSellerName(sale.getActor().getFullName());
    for (var item : items) {
      var line = new SaleLineJpaEntity();
      line.setSale(sale);
      line.setProduct(products.getReferenceById(item.productId()));
      line.setProductName(item.productName());
      line.setSku(item.sku());
      line.setQuantity(item.quantity());
      line.setUnitPrice(item.unitPrice());
      line.setSubtotal(item.subtotal());
      sale.getLines().add(line);
    }
    return sale(sales.saveAndFlush(sale));
  }

  @Override
  public Purchase savePurchase(Long actorId, String supplier, BigDecimal total, List<Line> items) {
    var purchase = new PurchaseJpaEntity();
    purchase.setActor(users.getReferenceById(actorId));
    purchase.setSupplierReference(supplier);
    purchase.setTotal(total);
    for (var item : items) {
      var line = new PurchaseLineJpaEntity();
      line.setPurchase(purchase);
      line.setProduct(products.getReferenceById(item.productId()));
      line.setProductName(item.productName());
      line.setSku(item.sku());
      line.setQuantity(item.quantity());
      line.setUnitPrice(item.unitPrice());
      line.setSubtotal(item.subtotal());
      purchase.getLines().add(line);
    }
    return purchase(purchases.saveAndFlush(purchase));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Sale> findSale(Long id) {
    return sales.findById(id).map(this::sale);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Purchase> findPurchase(Long id) {
    return purchases.findById(id).map(this::purchase);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<Sale> sales(Filter f, Long ownerId, int page, int size, String sort) {
    var paging =
        QuerySupport.page(
            page,
            size,
            sort,
            Map.of(
                "id",
                "id",
                "createdAt",
                "createdAt",
                "total",
                "total",
                "actor",
                "actor.username",
                "paymentMethod",
                "paymentMethod",
                "status",
                "status"));
    Specification<SaleJpaEntity> spec =
        (root, query, cb) -> {
          var predicates = common(root, cb, f);
          if (ownerId != null) predicates.add(cb.equal(root.get("customer").get("id"), ownerId));
          if (f.customerId() != null)
            predicates.add(cb.equal(root.get("customer").get("id"), f.customerId()));
          if (QuerySupport.supplied(f.paymentMethod()))
            predicates.add(cb.equal(root.get("paymentMethod"), f.paymentMethod()));
          if (QuerySupport.supplied(f.status()))
            predicates.add(cb.equal(root.get("status"), f.status()));
          return cb.and(predicates.toArray(Predicate[]::new));
        };
    return PageResponse.from(sales.findAll(spec, paging), this::sale);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<Purchase> purchases(Filter f, int page, int size, String sort) {
    var paging =
        QuerySupport.page(
            page,
            size,
            sort,
            Map.of(
                "id", "id", "createdAt", "createdAt", "total", "total", "actor", "actor.username"));
    Specification<PurchaseJpaEntity> spec =
        (root, query, cb) -> cb.and(common(root, cb, f).toArray(Predicate[]::new));
    return PageResponse.from(purchases.findAll(spec, paging), this::purchase);
  }

  private List<Predicate> common(
      jakarta.persistence.criteria.Root<?> root,
      jakarta.persistence.criteria.CriteriaBuilder cb,
      Filter f) {
    if (f.from() != null && f.to() != null && f.from().isAfter(f.to()))
      throw new DomainException("La fecha inicial no puede superar a la final.");
    if (f.minTotal() != null && f.maxTotal() != null && f.minTotal().compareTo(f.maxTotal()) > 0)
      throw new DomainException("El total mínimo no puede superar al máximo.");
    var result = new ArrayList<Predicate>();
    if (f.id() != null) result.add(cb.equal(root.get("id"), f.id()));
    if (f.actorId() != null) result.add(cb.equal(root.get("actor").get("id"), f.actorId()));
    if (f.from() != null)
      result.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay()));
    if (f.to() != null)
      result.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay()));
    if (f.minTotal() != null) result.add(cb.greaterThanOrEqualTo(root.get("total"), f.minTotal()));
    if (f.maxTotal() != null) result.add(cb.lessThanOrEqualTo(root.get("total"), f.maxTotal()));
    return result;
  }

  private Sale sale(SaleJpaEntity s) {
    return new Sale(
        s.getId(),
        s.getCreatedAt(),
        s.getTotal(),
        s.getActor().getId(),
        s.getActor().getFullName(),
        s.getCustomer() == null ? null : s.getCustomer().getId(),
        s.getPaymentMethod(),
        s.getStatus(),
        s.getLines().stream()
            .map(
                l ->
                    new Line(
                        l.getProduct().getId(),
                        l.getProductName() == null ? l.getProduct().getName() : l.getProductName(),
                        l.getSku() == null ? l.getProduct().getSku() : l.getSku(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getSubtotal()))
            .toList(),
        s.getCustomerName(),
        s.getSellerName() == null ? s.getActor().getFullName() : s.getSellerName(),
        s.getSubtotal(),
        s.getDiscount());
  }

  private Purchase purchase(PurchaseJpaEntity p) {
    return new Purchase(
        p.getId(),
        p.getCreatedAt(),
        p.getTotal(),
        p.getActor().getId(),
        p.getActor().getFullName(),
        p.getSupplierReference(),
        p.getLines().stream()
            .map(
                l ->
                    new Line(
                        l.getProduct().getId(),
                        l.getProductName() == null ? l.getProduct().getName() : l.getProductName(),
                        l.getSku() == null ? l.getProduct().getSku() : l.getSku(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getSubtotal()))
            .toList());
  }
}
