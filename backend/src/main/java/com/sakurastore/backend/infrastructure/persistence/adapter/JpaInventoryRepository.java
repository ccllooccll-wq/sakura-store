package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.port.InventoryRepositoryPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.infrastructure.persistence.entity.*;
import com.sakurastore.backend.infrastructure.persistence.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaInventoryRepository implements InventoryRepositoryPort {
  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager entityManager;
  private final CategoryJpaRepository categories;
  private final ProductJpaRepository products;
  private final StockJpaRepository stocks;
  private final MovementJpaRepository movements;
  private final SpringDataUserRepository users;

  public JpaInventoryRepository(
      CategoryJpaRepository categories,
      ProductJpaRepository products,
      StockJpaRepository stocks,
      MovementJpaRepository movements,
      SpringDataUserRepository users) {
    this.categories = categories;
    this.products = products;
    this.stocks = stocks;
    this.movements = movements;
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> categories() {
    return categories.findAllByActiveTrueOrderByNameAsc().stream()
        .map(c -> new Category(c.getId(), c.getName()))
        .toList();
  }

  @Override
  public Category createCategory(String name) {
    var category = new CategoryJpaEntity();
    category.setName(name.trim());
    category = categories.save(category);
    return new Category(category.getId(), category.getName());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> products(String query, boolean lowStock) {
    var f = new ProductFilter(query, null, null, null, null, null, null, lowStock, null);
    return products
        .findAll(ProductSpecifications.filter(f), Sort.by(Sort.Direction.DESC, "id"))
        .stream()
        .map(this::product)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<Product> searchProducts(
      ProductFilter filter, int page, int size, String sort) {
    var paging =
        QuerySupport.page(
            page,
            size,
            sort,
            Map.of(
                "id",
                "id",
                "sku",
                "sku",
                "name",
                "name",
                "category",
                "category.name",
                "price",
                "price",
                "stock",
                "stock.quantity",
                "minStock",
                "stock.minStock",
                "active",
                "active"));
    return PageResponse.from(
        products.findAll(ProductSpecifications.filter(filter), paging), this::product);
  }

  @Override
  @Transactional(readOnly = true)
  public InventorySummary summary() {
    var cb = entityManager.getCriteriaBuilder();
    var query = cb.createTupleQuery();
    var product = query.from(ProductJpaEntity.class);
    var stock = product.join("stock", jakarta.persistence.criteria.JoinType.LEFT);
    jakarta.persistence.criteria.Expression<Integer> quantity = cb.coalesce(stock.get("quantity"), 0);
    jakarta.persistence.criteria.Expression<Integer> minimum = cb.coalesce(stock.get("minStock"), 30);
    query.multiselect(
        cb.count(product).alias("active"),
        cb.sum(cb.<Long>selectCase().when(cb.lessThanOrEqualTo(quantity, minimum), 1L).otherwise(0L)).alias("low"),
        cb.sum(quantity.as(Long.class)).alias("units"),
        cb.sum(cb.prod(quantity.as(java.math.BigDecimal.class), product.<java.math.BigDecimal>get("price"))).alias("value"))
        .where(cb.isTrue(product.get("active")));
    var result = entityManager.createQuery(query).getSingleResult();
    return new InventorySummary(
        ((Number)result.get("active")).longValue(),
        result.get("low") == null ? 0 : ((Number)result.get("low")).longValue(),
        result.get("units") == null ? 0 : ((Number)result.get("units")).longValue(),
        result.get("value") == null ? java.math.BigDecimal.ZERO : (java.math.BigDecimal)result.get("value"));
  }

  @Override
  public Optional<Product> lockProduct(Long id) {
    return products.findLockedById(id).map(this::product);
  }

  @Override
  public Product create(ProductCommand command) {
    var p = new ProductJpaEntity();
    apply(p, command);
    var stock = new StockJpaEntity();
    stock.setProduct(p);
    stock.setMinStock(command.minStock());
    p.setStock(stock);
    return product(products.saveAndFlush(p));
  }

  @Override
  public Product update(Long id, ProductCommand command) {
    var p = locked(id);
    apply(p, command);
    stock(p).setMinStock(command.minStock());
    return product(p);
  }

  private void apply(ProductJpaEntity p, ProductCommand command) {
    p.setSku(command.sku().trim().toUpperCase(Locale.ROOT));
    p.setName(command.name().trim());
    p.setCategory(
        categories
            .findById(command.categoryId())
            .orElseThrow(() -> new DomainException("La categoría no existe.")));
    p.setPrice(command.price());
  }

  @Override
  public void setActive(Long id, boolean active) {
    locked(id).setActive(active);
  }

  @Override
  public boolean categoryExists(Long id) {
    return categories.existsByIdAndActiveTrue(id);
  }

  @Override
  public void setStock(Long id, int quantity) {
    if (quantity < 0) throw new DomainException("El stock no puede ser negativo.");
    stock(locked(id)).setQuantity(quantity);
  }

  @Override
  public Movement recordMovement(
      Product product, MovementCommand command, int next, Long actorId, String actorName) {
    var p = locked(product.id());
    if (actorId == null)
      throw new DomainException("El movimiento necesita un usuario responsable.");
    var m = new MovementJpaEntity();
    m.setProduct(p);
    m.setActor(users.getReferenceById(actorId));
    m.setType(command.type());
    m.setQuantity(command.quantity());
    m.setReason(command.reason().trim());
    m.setPreviousStock(product.stock());
    m.setNewStock(next);
    stock(p).setQuantity(next);
    return movement(movements.saveAndFlush(m));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Movement> movements(Long productId, int page) {
    var paging = PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "id"));
    var found =
        productId == null
            ? movements.findAll(paging)
            : movements.findAllByProductId(productId, paging);
    return found.stream().map(this::movement).toList();
  }

  private ProductJpaEntity locked(Long id) {
    return products
        .findLockedById(id)
        .orElseThrow(() -> new DomainException("Producto no encontrado."));
  }

  private StockJpaEntity stock(ProductJpaEntity p) {
    if (p.getStock() == null) {
      var s = new StockJpaEntity();
      s.setProduct(p);
      s.setMinStock(30);
      p.setStock(stocks.save(s));
    }
    return p.getStock();
  }

  private Product product(ProductJpaEntity p) {
    var s = p.getStock();
    return new Product(
        p.getId(),
        p.getSku(),
        p.getName(),
        p.getCategory().getId(),
        p.getCategory().getName(),
        p.getPrice(),
        s == null ? 0 : s.getMinStock(),
        s == null ? 0 : s.getQuantity(),
        p.isActive());
  }

  private Movement movement(MovementJpaEntity m) {
    return new Movement(
        m.getId(),
        m.getProduct().getId(),
        m.getProduct().getName(),
        m.getType(),
        m.getQuantity(),
        m.getPreviousStock() == null ? 0 : m.getPreviousStock(),
        m.getNewStock() == null ? 0 : m.getNewStock(),
        m.getReason(),
        m.getActor() == null ? "Sistema" : m.getActor().getFullName(),
        m.getCreatedAt());
  }
}
