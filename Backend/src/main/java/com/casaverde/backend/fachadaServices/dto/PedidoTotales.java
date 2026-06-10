package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;

/**
 * Totales calculados de un pedido. Producido por PedidoServiceImpl.calcularTotales()
 * y consumido por PedidoMapper.toDTO() para mantener el mapper libre de aritmética.
 */
public record PedidoTotales(
        BigDecimal totalCop,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente,
        BigDecimal valorCambio
) {
    public static PedidoTotales of(Pedido pedido) {
        BigDecimal total = pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonado = pedido.getPagos().stream()
                .map(Pago::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PedidoTotales(
                total,
                abonado,
                total.subtract(abonado).max(BigDecimal.ZERO),
                abonado.subtract(total).max(BigDecimal.ZERO));
    }
}
