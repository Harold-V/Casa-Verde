package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vacacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
