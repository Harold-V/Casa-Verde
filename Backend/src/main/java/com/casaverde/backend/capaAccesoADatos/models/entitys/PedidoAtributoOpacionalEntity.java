package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PedidoAtributoOpcional")
public class PedidoAtributoOpacionalEntity {

    @EmbeddedId
    private PedidoAtributoOpcionalKey id;

    @ManyToOne
    @MapsId("pedID")
    @JoinColumn(name = "pedID", nullable = false)
    private PedidoEntity pedido;

    @Column(nullable = false)
    private String pedAtribOpcValor;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Embeddable
    public static class PedidoAtributoOpcionalKey implements Serializable {

        @Column(name = "pedID")
        private Long pedID;

        @Column(name = "pedAtribOpcClave")
        private String pedAtribOpcClave;

    }

}
