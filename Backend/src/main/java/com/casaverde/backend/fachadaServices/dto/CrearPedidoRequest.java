package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoRequest {
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;
    private Long salonId;
    private String mesa;
    private String fechaReserva;
    private String horaReserva;
    private Integer cantPersonas;
    @Positive(message = "El abono debe ser mayor a 0")
    private BigDecimal abono;
    private String monedaAbono;
    private String observacion;
    private Boolean paraLlevar;
    @Valid
    private List<DetalleItemRequest> detalles;
}
