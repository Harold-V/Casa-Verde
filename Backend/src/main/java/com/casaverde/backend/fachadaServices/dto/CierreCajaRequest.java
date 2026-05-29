package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaRequest {
    private LocalDate fecha;
    private Long usuarioId;
    private BigDecimal contadoCop;
    private BigDecimal contadoUsd;
    private BigDecimal contadoNequi;
    private String observacion;
}
