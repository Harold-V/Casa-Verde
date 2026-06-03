package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PagoEmpleadoRequest {
    private String moneda;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String observacion;
}
