package com.casaverde.backend.capaAccesoADatos.models.Entitys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "PedidoAtributoOpcional")
public class PedidoAtributoOpcionalEntity {

    @Id
    @Column(nullable = false, length = 255)
    private String pedAtrOpcClave;

    @Column(nullable = false, length = 255)
    private String pedAtrOpcValor;

    @ManyToOne
    @MapsId("pedID")
    @JoinColumn(name = "pedID", nullable = false)
    private PedidoEntity pedido;

}
