package com.casaverde.backend.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private String insumo;

    @Column(name = "stock_actual", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String unidad = "unidad";

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void onSave() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
