package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venta")
@Getter
@Setter
@NoArgsConstructor
public class SaleJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_venta")
  private Long id;

  @Column(name = "fecha", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "total", nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "cliente_nombre", length = 200)
  private String customerName;

  @Column(name = "vendedor_nombre", length = 200)
  private String sellerName;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_usuario", nullable = false)
  private UserJpaEntity actor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_cliente")
  private UserJpaEntity customer;

  @Column(name = "metodo_pago", nullable = false, length = 20)
  private String paymentMethod;

  @Column(name = "estado", nullable = false, length = 20)
  private String status;

  @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<SaleLineJpaEntity> lines = new ArrayList<>();
}
