package com.casaverde.backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoRequest {
    private String insumo;
    private String tipo;      // entrada | ajuste
    private Double cantidad;
    private String observacion;
}
