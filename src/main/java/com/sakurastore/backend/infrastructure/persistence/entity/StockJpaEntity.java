package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class StockJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_stock")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_producto", nullable = false, unique = true)
  private ProductJpaEntity product;

  @Column(name = "cantidad", nullable = false)
  private int quantity;

  @Column(name = "stock_minimo", nullable = false)
  private int minStock = 30;
}
