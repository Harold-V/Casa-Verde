package com.casaverde.backend.capaAccesoADatos.models;

import java.sql.Date;
import java.sql.Time;

import com.casaverde.backend.capaAccesoADatos.models.enums.estadoPedido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "Pedido") // Nombre de la tabla en la base de datos
public class PedidoEntity {

    @Id
    private Long pedID;

    @Column(nullable = false)
    private String pedNombreCliente;

    @Column(nullable = false)
    private Date pedFecha;

    @Column(nullable = false)
    private Time pedHora;

    @Column(nullable = false)
    private Double pedValorTotal;

    @Column(nullable = false)
    private estadoPedido pedEstado;

    // Constructor sin parámetros
    public PedidoEntity() {
    }
}
