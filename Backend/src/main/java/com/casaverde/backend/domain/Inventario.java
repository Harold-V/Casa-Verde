package com.casaverde.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String insumo;

    @Column(name = "stock_actual", nullable = false)
    private Double stockActual = 0.0;

    @Column(nullable = false)
    private String unidad = "unidad";

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void onSave() {
        actualizadoEn = LocalDateTime.now();
    }
}
