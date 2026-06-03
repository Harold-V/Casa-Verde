package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "proveedor_nombre", nullable = false)
    private String proveedorNombre;

    @Column(nullable = false)
    private String insumo;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", precision = 19, scale = 0, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "total_cop", precision = 19, scale = 0, nullable = false)
    private BigDecimal totalCop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado;

    private String observacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
