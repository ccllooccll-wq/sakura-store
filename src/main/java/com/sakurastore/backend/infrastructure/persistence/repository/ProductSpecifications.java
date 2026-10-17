package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.application.dto.ProductFilter;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.infrastructure.persistence.entity.*;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {
  private ProductSpecifications() {}

  public static Specification<ProductJpaEntity> filter(ProductFilter f) {
    if (f.minPrice() != null && f.maxPrice() != null && f.minPrice().compareTo(f.maxPrice()) > 0)
      throw new DomainException("El precio mínimo no puede superar al máximo.");
    return (root, query, cb) -> {
      var category = root.join("category", JoinType.INNER);
      var stock = root.join("stock", JoinType.LEFT);
      var predicates = new ArrayList<Predicate>();
      if (QuerySupport.supplied(f.q())) {
        String like = QuerySupport.contains(f.q());
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), like, '\\'),
                cb.like(cb.lower(root.get("sku")), like, '\\'),
                cb.like(cb.lower(category.get("name")), like, '\\')));
      }
      if (QuerySupport.supplied(f.sku()))
        predicates.add(cb.like(cb.lower(root.get("sku")), QuerySupport.contains(f.sku()), '\\'));
      if (QuerySupport.supplied(f.name()))
        predicates.add(cb.like(cb.lower(root.get("name")), QuerySupport.contains(f.name()), '\\'));
      if (f.categoryId() != null) predicates.add(cb.equal(category.get("id"), f.categoryId()));
      if (f.minPrice() != null)
        predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.minPrice()));
      if (f.maxPrice() != null)
        predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.maxPrice()));
      if (f.active() != null) predicates.add(cb.equal(root.get("active"), f.active()));
      Expression<Integer> quantity = cb.coalesce(stock.get("quantity"), 0);
      Expression<Integer> minimum = cb.coalesce(stock.get("minStock"), 0);
      if (f.lowStock()) {
        predicates.add(cb.isTrue(root.get("active")));
        predicates.add(cb.lessThanOrEqualTo(quantity, minimum));
      }
      if (f.available() != null)
        predicates.add(f.available() ? cb.greaterThan(quantity, 0) : cb.equal(quantity, 0));
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
