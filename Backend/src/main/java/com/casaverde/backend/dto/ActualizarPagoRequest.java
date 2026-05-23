package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPagoRequest {
    private String moneda;  // COP, USD, NEQUI
    private BigDecimal monto;
}
