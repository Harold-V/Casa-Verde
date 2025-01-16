package com.casaverde.backend.fachadaServices.DTO;

import java.sql.Date;
import java.sql.Time;
import com.casaverde.backend.capaAccesoADatos.models.enums.estadoPedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {

    private Long pedID;
    private String pedNombreCliente;
    private Date pedFecha;
    private Time pedHora;
    private Double pedValorTotal;
    private estadoPedido pedEstado;

}
