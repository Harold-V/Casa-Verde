package com.casaverde.backend.capaAccesoADatos.models.Relations;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PedidoProducto") // Nombre de la tabla en la base de datos
public class PedidoProductoRelation {

    @EmbeddedId
    private PedidoProductoKey id;

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

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PedidoProductoKey implements Serializable {

        @Column(name = "prodID")
        private Long prodID;

        @Column(name = "pedID")
        private Long pedID;
    }
}
