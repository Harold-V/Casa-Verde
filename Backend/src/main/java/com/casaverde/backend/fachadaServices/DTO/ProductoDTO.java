package com.casaverde.backend.fachadaServices.DTO;

import com.casaverde.backend.capaAccesoADatos.models.Enums.estadoProducto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private Long prodID; // ID del producto
    private String prodNombre; // Nombre del producto
    private Double prodPrecio; // Precio del producto
    private estadoProducto prodEstado; // Estado del producto
}
