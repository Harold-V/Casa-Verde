package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComposicionDTO {
    private Long id;
    private String insumo;
    private BigDecimal cantidad;
}
