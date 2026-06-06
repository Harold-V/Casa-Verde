package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;

/**
 * Totales calculados de un pedido. Producido por PedidoServiceImpl.calcularTotales()
 * y consumido por PedidoMapper.toDTO() para mantener el mapper libre de aritmética.
 */
public record PedidoTotales(
        BigDecimal totalCop,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente,
        BigDecimal valorCambio
) {}
