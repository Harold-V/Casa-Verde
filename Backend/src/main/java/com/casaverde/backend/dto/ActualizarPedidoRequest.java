package com.casaverde.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPedidoRequest {
    private String nombre;
    private String fechaReserva;   // null = quitar fecha
    private String horaReserva;    // null = quitar hora
    private String mesa;
    private Integer cantPersonas;
    private String observacion;
    private Long salonId;          // null = sin salón
}
