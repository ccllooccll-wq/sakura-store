package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_compra")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseLineJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_detalle_compra")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_compra", nullable = false)
  private PurchaseJpaEntity purchase;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_producto", nullable = false)
  private ProductJpaEntity product;

  @Column(name = "producto_nombre", length = 150)
  private String productName;

  @Column(name = "producto_codigo", length = 50)
  private String sku;

  @Column(name = "cantidad", nullable = false)
  private int quantity;

  @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;
}
