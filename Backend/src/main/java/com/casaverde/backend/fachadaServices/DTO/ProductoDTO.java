package com.casaverde.backend.fachadaServices.DTO;

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
}
