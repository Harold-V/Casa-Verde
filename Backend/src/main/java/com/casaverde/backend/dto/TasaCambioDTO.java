package com.casaverde.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TasaCambioDTO {
    private Long id;
    private BigDecimal valorCop;
    private LocalDateTime vigenteDesde;
    private LocalDateTime creadoEn;
}
