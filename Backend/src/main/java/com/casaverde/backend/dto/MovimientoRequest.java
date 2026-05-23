package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoRequest {
    private String insumo;
    private String tipo;      // entrada | ajuste
    private BigDecimal cantidad;
    private String observacion;
}
