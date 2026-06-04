package com.casaverde.backend.fachadaServices.mapper;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDetalleDTO;

/**
 * Convierte entidades de Pedido a DTOs de respuesta.
 * Los cálculos de total y abonado son responsabilidad de este mapper
 * ya que son operaciones de lectura puras sobre las colecciones del pedido.
 */
@Component
public class PedidoMapper {

    public PedidoDTO toDTO(Pedido p) {
        BigDecimal total = calcularTotal(p);
        BigDecimal abonado = calcularAbonado(p);
        BigDecimal saldoPendiente = total.subtract(abonado).max(BigDecimal.ZERO);
        BigDecimal valorCambio = abonado.subtract(total).max(BigDecimal.ZERO);

        return PedidoDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .salonId(p.getSalon() != null ? p.getSalon().getId() : null)
                .salonNombre(p.getSalon() != null ? p.getSalon().getNombre() : null)
                .mesa(p.getMesa())
                .fechaReserva(p.getFechaReserva() != null ? p.getFechaReserva().toString() : null)
                .horaReserva(p.getHoraReserva() != null ? p.getHoraReserva().toString() : null)
                .cantPersonas(p.getCantPersonas())
                .estado(p.getEstado() != null ? p.getEstado().name() : null)
                .observacion(p.getObservacion())
                .paraLlevar(Boolean.TRUE.equals(p.getParaLlevar()))
                .totalCop(total)
                .totalAbonado(abonado)
                .saldoPendiente(saldoPendiente)
                .valorCambio(valorCambio)
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .finalizadoEn(p.getFinalizadoEn())
                .detalles(p.getDetalles().stream().map(this::toDetalleDTO).collect(Collectors.toList()))
                .pagos(p.getPagos().stream().map(this::toPagoDTO).collect(Collectors.toList()))
                .build();
    }

    public PedidoDetalleDTO toDetalleDTO(PedidoDetalle d) {
        return PedidoDetalleDTO.builder()
                .id(d.getId())
                .productoId(d.getProducto() != null ? d.getProducto().getId() : null)
                .nombreSnapshot(d.getNombreSnapshot())
                .precioUnitarioCop(d.getPrecioUnitarioCop())
                .cantidad(d.getCantidad())
                .subtotalCop(d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .composicionSnapshot(d.getComposicionSnapshot())
                .build();
    }

    public PagoDTO toPagoDTO(Pago p) {
        return PagoDTO.builder()
                .id(p.getId())
                .moneda(p.getMoneda())
                .monto(p.getMonto())
                .montoCop(p.getMontoCop())
                .tasaUsada(p.getTasaUsada())
                .registradoEn(p.getRegistradoEn())
                .build();
    }

    /** Suma el subtotal de todos los ítems del pedido. */
    public BigDecimal calcularTotal(Pedido pedido) {
        return pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Suma el montoCop de todos los pagos del pedido. */
    public BigDecimal calcularAbonado(Pedido pedido) {
        return pedido.getPagos().stream()
                .map(Pago::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
