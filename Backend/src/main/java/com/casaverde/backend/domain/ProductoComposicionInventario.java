package com.casaverde.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "producto_composicion_inventario",
    uniqueConstraints = @UniqueConstraint(columnNames = {"producto_id", "insumo"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductoComposicionInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String insumo;

    @Column(nullable = false)
    private Double cantidad;
}
