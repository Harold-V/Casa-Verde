package com.casaverde.backend.fachadaServices.DTO;

import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoProducto;

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

    private EstadoProducto prodEstado; // Estado del producto

}
