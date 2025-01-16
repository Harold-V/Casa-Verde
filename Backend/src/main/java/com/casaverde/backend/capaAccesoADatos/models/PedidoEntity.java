package com.casaverde.backend.capaAccesoADatos.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.casaverde.backend.capaAccesoADatos.models.enums.estadoPedido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private estadoPedido pedEstado;

    // Getters y Setters con formato local Date para busqueda de pedidos por fecha
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
