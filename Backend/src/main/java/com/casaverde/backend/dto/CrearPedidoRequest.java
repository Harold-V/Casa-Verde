package com.casaverde.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CrearPedidoRequest {
    private String nombre;         // obligatorio
    private Long   salonId;
    private String mesa;
    private String fechaReserva;   // "yyyy-MM-dd", opcional
    private String horaReserva;    // "HH:mm",      opcional
    private Integer cantPersonas;  // opcional
    private BigDecimal  abono;     // pago inicial,  opcional
    private String  monedaAbono;   // "COP" | "USD" | "NEQUI", default COP
    private String observacion;
    private List<DetalleItemRequest> detalles;
}
