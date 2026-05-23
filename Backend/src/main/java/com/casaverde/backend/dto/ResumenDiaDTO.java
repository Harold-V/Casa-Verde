package com.casaverde.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResumenDiaDTO {

    private String fecha; // yyyy-MM-dd

    private long pedidosTotales;
    private long pedidosAbiertos;
    private long pedidosFinalizados;
    private long pedidosAnulados;

    private BigDecimal cobradoCop;
    private BigDecimal cobradoUsd;
    private BigDecimal cobradoNequi;
    private BigDecimal cobradoTotalCop;

    private BigDecimal gastosCop;
    private BigDecimal gastosUsd;
    private BigDecimal gastosNequi;
    private BigDecimal gastosTotalCop;

    private BigDecimal balanceNeto;

    private List<FondoDTO> fondos;
    private List<PedidoDTO> pedidos;
    private List<GastoDTO> gastos;
}
