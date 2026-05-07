package com.casaverde.backend.dto;

import java.util.List;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductoRequest {
    private String nombre;
    private Double precio;
    private Long categoriaId;
    private Boolean afectaInventario;
    private Boolean activo;
    private List<ComposicionDTO> composicion;
}
