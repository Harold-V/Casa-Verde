package com.casaverde.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tasa_cambio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TasaCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valor_cop", nullable = false)
    private Double valorCop;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDateTime vigentDesde;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    void onCreate() {
        creadoEn = LocalDateTime.now();
        if (vigentDesde == null) {
            vigentDesde = LocalDateTime.now();
        }
    }
}
