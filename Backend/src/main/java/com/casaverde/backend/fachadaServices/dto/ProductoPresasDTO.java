package com.casaverde.backend.fachadaServices.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoPresasDTO {

    private String recurso; // "cantCuyes" o "cantGallinas"
    private Integer cantidad;
}
