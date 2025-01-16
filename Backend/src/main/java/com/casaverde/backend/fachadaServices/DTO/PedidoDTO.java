package com.casaverde.backend.fachadaServices.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

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
    private LocalDate pedFecha;
    private LocalTime pedHora;
    private Double pedValorTotal;
    private estadoPedido pedEstado;

}
