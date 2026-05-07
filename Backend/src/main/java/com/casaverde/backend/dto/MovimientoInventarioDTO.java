package com.casaverde.backend.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInventarioDTO {
    private Long id;
    private String insumo;
    private String tipo;
    private Double cantidad;
    private Double stockResultante;
    private String referenciaTipo;
    private Long referenciaId;
    private String observacion;
    private LocalDateTime registradoEn;
}
