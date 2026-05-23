package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RegistrarPagoRequest {
    private String moneda; // COP, USD, NEQUI
    private BigDecimal monto;
}
