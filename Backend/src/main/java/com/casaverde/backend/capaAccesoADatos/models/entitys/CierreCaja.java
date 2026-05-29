package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cierre_caja", uniqueConstraints = @UniqueConstraint(columnNames = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal esperadoCop;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal esperadoUsd;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal esperadoNequi;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal contadoCop;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal contadoUsd;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal contadoNequi;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal diferenciaCop;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal diferenciaUsd;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal diferenciaNequi;

    @Column(name = "cobrado_total_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal cobradoTotalCop;

    @Column(name = "gastos_total_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal gastosTotalCop;

    @Column(name = "balance_neto", nullable = false, precision = 19, scale = 0)
    private BigDecimal balanceNeto;

    private String observacion;

    @Column(name = "backup_path")
    private String backupPath;

    @Column(name = "cerrado_en", nullable = false)
    private LocalDateTime cerradoEn;

    @PrePersist
    void onCreate() {
        cerradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
