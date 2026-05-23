package com.casaverde.backend.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fondo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Fondo {

    @Id
    private String moneda; // COP, USD, NEQUI

    @Version
    private Long version;

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
