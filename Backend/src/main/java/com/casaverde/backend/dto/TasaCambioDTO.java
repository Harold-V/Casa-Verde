package com.casaverde.backend.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TasaCambioDTO {
    private Long id;
    private Double valorCop;
    private LocalDateTime vigentDesde;
    private LocalDateTime creadoEn;
}
