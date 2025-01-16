package com.casaverde.backend.fachadaServices.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoAdicionalDTO {
    private Long prodAdID; // ID del productoAdicional
    private String prodAdNombre; // Nombre del productoAdicional
    private Double prodAdPrecio; // Precio del productoAdicional
}
