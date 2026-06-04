package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CuentaCobrarRequest {
    private String clienteNombre;
    private String concepto;
    private BigDecimal monto;
    private String celular;
    private LocalDate fecha;
    private String observacion;
}
