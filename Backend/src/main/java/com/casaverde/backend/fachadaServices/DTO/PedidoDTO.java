package com.casaverde.backend.fachadaServices.DTO;

import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoPedido;

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
    private EstadoPedido pedEstado;

    private List<PedidoProductoDTO> productos;
    private List<PedidoAtributoOpcionalDTO> atributosOpcionales;
}
