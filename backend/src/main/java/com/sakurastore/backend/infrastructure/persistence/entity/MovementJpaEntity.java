package com.sakurastore.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movimiento_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovementJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_movimiento")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_producto", nullable = false)
  private ProductJpaEntity product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario")
  private UserJpaEntity actor;

  @Column(name = "tipo", nullable = false, length = 10)
  private String type;

  @Column(name = "cantidad", nullable = false)
  private int quantity;

  @Column(name = "fecha", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "descripcion", length = 250)
  private String reason;

  @Column(name = "stock_anterior")
  private Integer previousStock;

  @Column(name = "stock_nuevo")
  private Integer newStock;
}
