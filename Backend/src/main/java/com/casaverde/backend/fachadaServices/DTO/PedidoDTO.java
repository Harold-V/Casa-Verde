package com.casaverde.backend.fachadaServices.DTO;

import com.casaverde.backend.capaAccesoADatos.models.enums.estadoPedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {

    private Long pedID;
    private String pedNombreCliente;
    private String pedFecha;
    private String pedHora;
    private Double pedValorTotal;
    private estadoPedido pedEstado;

    private List<PedidoProductoDTO> productos;
}
