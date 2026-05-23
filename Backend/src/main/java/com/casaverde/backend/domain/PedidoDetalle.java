package com.casaverde.backend.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PedidoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "nombre_snapshot", nullable = false)
    private String nombreSnapshot;

    @Column(name = "precio_unitario_cop", nullable = false)
    private BigDecimal precioUnitarioCop;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "composicion_snapshot", columnDefinition = "TEXT")
    private String composicionSnapshot;
}
