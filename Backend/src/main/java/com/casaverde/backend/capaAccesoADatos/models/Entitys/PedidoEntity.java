package com.casaverde.backend.capaAccesoADatos.models.Entitys;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.ArrayList;

import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.models.Relations.PedidoProductoRelation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Pedido") // Nombre de la tabla en la base de datos
public class PedidoEntity {

    @Id
    private Long pedID;

    @Column(nullable = false)
    private String pedNombreCliente;

    @Column(nullable = false)
    private String pedFecha;

    @Column(nullable = false)
    private String pedHora;

    @Column(nullable = false)
    private Double pedValorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido pedEstado;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoAtributoOpacionalEntity> atributosOpcionales = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoEntity> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PedidoProductoRelation> productos = new ArrayList<>();

    // Getters y Setters con formato local Date para búsqueda de pedidos por fecha
    public LocalDate getPedFechaAsLocalDate() {
        return LocalDate.parse(pedFecha, DateTimeFormatter.ISO_DATE);
    }

    public void setPedFechaFromLocalDate(LocalDate date) {
        this.pedFecha = date.format(DateTimeFormatter.ISO_DATE);
    }

    public LocalTime getPedHoraAsLocalTime() {
        return LocalTime.parse(pedHora, DateTimeFormatter.ISO_TIME);
    }

    public void setPedHoraFromLocalTime(LocalTime time) {
        this.pedHora = time.format(DateTimeFormatter.ISO_TIME);
    }
}
