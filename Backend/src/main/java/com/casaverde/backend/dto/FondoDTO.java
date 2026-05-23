package com.casaverde.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FondoDTO {
    private String moneda;
    private BigDecimal saldo;
    private LocalDateTime actualizadoEn;
}
