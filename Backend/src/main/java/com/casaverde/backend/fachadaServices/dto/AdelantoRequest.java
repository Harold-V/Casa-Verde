package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdelantoRequest {
    private BigDecimal monto;
    private String moneda;
    private LocalDate fecha;
    private String observacion;
}
