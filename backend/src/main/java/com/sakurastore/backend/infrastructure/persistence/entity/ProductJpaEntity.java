package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class ProductJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_producto")
  private Long id;

  @Column(name = "codigo", nullable = false, unique = true, length = 50)
  private String sku;

  @Column(name = "nombre", nullable = false, length = 150)
  private String name;

  @Column(name = "descripcion", length = 255)
  private String description;

  @Column(name = "precio", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "activo", nullable = false)
  private boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_categoria", nullable = false)
  private CategoryJpaEntity category;

  @OneToOne(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private StockJpaEntity stock;
}
