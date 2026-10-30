package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_compra")
  private Long id;

  @Column(name = "fecha", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "total", nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_usuario", nullable = false)
  private UserJpaEntity actor;

  @Column(name = "referencia_proveedor", length = 200)
  private String supplierReference;

  @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<PurchaseLineJpaEntity> lines = new ArrayList<>();
}
