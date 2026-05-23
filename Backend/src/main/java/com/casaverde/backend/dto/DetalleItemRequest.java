package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class DetalleItemRequest {
    private Long productoId;
    private String nombreManual;
    private BigDecimal precioManual;
    private Integer cantidad;
}
