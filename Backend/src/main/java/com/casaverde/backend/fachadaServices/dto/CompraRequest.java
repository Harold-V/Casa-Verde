package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequest {
    private Long proveedorId;
    private String insumo;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private String observacion;
    private LocalDate fecha;
}
