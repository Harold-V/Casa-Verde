package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AjusteCapitalRequest {
    private BigDecimal nuevoSaldo;
    private String motivo;
}
