package com.casaverde.backend.dto;

import java.math.BigDecimal;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class GastoRequest {
    private String concepto;
    private String categoria; // insumos, servicios, equipos, otros
    private String moneda;    // COP, USD, NEQUI
    private BigDecimal monto;
    private String insumoInventario;      // gallina, cuy u otro insumo inventariable
    private BigDecimal cantidadInventario; // entrada de stock asociada al gasto
    private String observacion;
}
