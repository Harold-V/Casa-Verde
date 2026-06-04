package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoAdelanto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdelantoRequest {
    private BigDecimal monto;
    private String moneda;
    private LocalDate fecha;
    private String observacion;
    private TipoAdelanto tipo;
}
