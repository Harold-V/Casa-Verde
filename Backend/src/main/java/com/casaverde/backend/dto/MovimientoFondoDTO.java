package com.casaverde.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MovimientoFondoDTO {
    private String tipo;       // "cobro" | "gasto"
    private String moneda;
    private BigDecimal monto;
    private BigDecimal montoCop;
    private String concepto;
    private LocalDateTime fecha;
}
