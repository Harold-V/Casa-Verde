package com.casaverde.backend.capaAccesoADatos.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Entity
@Table(name = "PedidoProducto") // Nombre de la tabla en la base de datos
public class PedidoProductoEntity {

    @EmbeddedId
    private PedidoProductoId id;

    @Column(nullable = false)
    private Integer pedProdCantidad;

    @ManyToOne
    @MapsId("prodID")
    @JoinColumn(name = "prodID", nullable = false)
    private ProductoEntity producto;

    @ManyToOne
    @MapsId("pedID")
    @JoinColumn(name = "pedID", nullable = false)
    private PedidoEntity pedido;

    // Constructor sin parámetros (requerido por JPA)
    public PedidoProductoEntity() {
    }

    @Embeddable
    @Data
    @AllArgsConstructor
    public static class PedidoProductoId implements Serializable {

        @Column(name = "prodID")
        private Long prodID;

        @Column(name = "pedID")
        private Long pedID;

        // Constructor sin parámetros (requerido por JPA)
        public PedidoProductoId() {
        }
    }
}
