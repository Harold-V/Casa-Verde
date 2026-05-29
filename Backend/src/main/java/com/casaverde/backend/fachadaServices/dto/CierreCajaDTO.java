package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaDTO {
    private Long id;
    private LocalDate fecha;
    private Long usuarioId;
    private String usuarioNombre;
    private BigDecimal esperadoCop;
    private BigDecimal esperadoUsd;
    private BigDecimal esperadoNequi;
    private BigDecimal contadoCop;
    private BigDecimal contadoUsd;
    private BigDecimal contadoNequi;
    private BigDecimal diferenciaCop;
    private BigDecimal diferenciaUsd;
    private BigDecimal diferenciaNequi;
    private BigDecimal cobradoTotalCop;
    private BigDecimal gastosTotalCop;
    private BigDecimal balanceNeto;
    private String observacion;
    private String backupPath;
    private LocalDateTime cerradoEn;
}
