package com.casaverde.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movimiento_inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String insumo;

    @Column(nullable = false)
    private String tipo; // entrada | salida | ajuste

    @Column(nullable = false)
    private Double cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Double stockResultante;

    @Column(name = "referencia_tipo")
    private String referenciaTipo; // pedido | gasto | ajuste_manual

    @Column(name = "referencia_id")
    private Long referenciaId;

    private String observacion;

    @Column(name = "registrado_en")
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        registradoEn = LocalDateTime.now();
    }
}
