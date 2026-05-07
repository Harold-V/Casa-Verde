package com.casaverde.backend.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventarioDTO {
    private Long id;
    private String insumo;
    private Double stockActual;
    private String unidad;
    private LocalDateTime actualizadoEn;
}
