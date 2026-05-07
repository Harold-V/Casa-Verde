package com.casaverde.backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalonDTO {
    private Long id;
    private String nombre;
    private Integer aforo;
    private Boolean activo;
}
