package com.casaverde.backend.capaAccesoADatos.models.entitys;

import com.casaverde.backend.capaAccesoADatos.models.enums.PagoTipo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Pago") // Nombre de la tabla en la base de datos
public class PagoEntity {

    @Id
    private Long pagoID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedID")
    private PedidoEntity pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagoTipo pagoTipo;

    @Column(nullable = false)
    private Double pagoValor;
}