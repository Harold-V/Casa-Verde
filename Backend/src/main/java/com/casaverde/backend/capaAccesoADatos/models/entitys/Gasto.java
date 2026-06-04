package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gasto", indexes = {
        @Index(name = "idx_gasto_registrado_en", columnList = "registrado_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false)
    private String categoria; // insumos, servicios, equipos, otros

    @Column(nullable = false)
    private String moneda; // COP, USD, NEQUI

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada")
    private BigDecimal tasaUsada;

    @Column(name = "insumo_inventario")
    private String insumoInventario;

    @Column(name = "cantidad_inventario", precision = 19, scale = 4)
    private BigDecimal cantidadInventario;

    private String observacion;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        registradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
