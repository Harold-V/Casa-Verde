package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fondo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fondo {

    @Id
    private String moneda; // COP, USD, NEQUI, NEQUI_2, NEQUI_HAROLD, etc.

    @Version
    private Long version;

    /**
     * Nombre amigable para mostrar al usuario. Obligatorio para fondos NEQUI
     * adicionales.
     */
    private String alias;

    @Column(nullable = false)
    private BigDecimal saldo;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    void onCreate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
