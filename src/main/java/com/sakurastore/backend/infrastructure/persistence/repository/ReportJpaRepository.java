package com.sakurastore.backend.infrastructure.persistence.repository;
import com.sakurastore.backend.infrastructure.persistence.entity.SaleJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReportJpaRepository extends JpaRepository<SaleJpaEntity, Long> {
  interface SalesTotals { long getSalesCount(); BigDecimal getSubtotal(); BigDecimal getDiscount(); BigDecimal getTotal(); }
  interface ProductTotals { Long getProductId(); String getSku(); String getName(); long getUnits(); BigDecimal getGrossAmount(); }
  @Query("""
      select count(s) as salesCount, sum(s.subtotal) as subtotal, sum(s.discount) as discount, sum(s.total) as total
      from SaleJpaEntity s where s.status = 'COMPLETED' and s.createdAt >= :from and s.createdAt < :until
      """)
  SalesTotals summary(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);
  @Query(value = """
      select p.id as productId, p.sku as sku, p.name as name, sum(l.quantity) as units, sum(l.subtotal) as grossAmount
      from SaleLineJpaEntity l join l.product p join l.sale s
      where s.status = 'COMPLETED' and s.createdAt >= :from and s.createdAt < :until
      group by p.id, p.sku, p.name order by sum(l.quantity) desc, p.id asc
      """, countQuery = """
      select count(distinct l.product.id) from SaleLineJpaEntity l join l.sale s
      where s.status = 'COMPLETED' and s.createdAt >= :from and s.createdAt < :until
      """)
  Page<ProductTotals> topProducts(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until, Pageable pageable);
}
