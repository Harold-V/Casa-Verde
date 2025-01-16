package com.casaverde.backend.fachadaServices.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoProductoDTO {

    private String prodNombre;
    private Double prodPrecio;
    private Integer pedProdCantidad;

}