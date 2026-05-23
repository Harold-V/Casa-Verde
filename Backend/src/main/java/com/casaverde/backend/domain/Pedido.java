package com.casaverde.backend.domain;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "pedido")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    private String mesa;

    @Column(name = "fecha_reserva")
    private String fechaReserva; // "yyyy-MM-dd"

    @Column(name = "hora_reserva")
    private String horaReserva;  // "HH:mm"

    @Column(name = "cant_personas")
    private Integer cantPersonas;

    @Column(nullable = false)
    private String estado; // PENDIENTE, ABONADO_PARCIAL, PAGADO, FINALIZADO, ANULADO

    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<PedidoDetalle> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<Pago> pagos = new ArrayList<>();

    @PrePersist
    void onCreate() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
        if (estado == null) estado = EstadoPedido.PENDIENTE.name();
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
