package com.casaverde.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.Fondo;
import com.casaverde.backend.domain.Gasto;
import com.casaverde.backend.domain.EstadoPedido;
import com.casaverde.backend.domain.Pago;
import com.casaverde.backend.domain.Pedido;
import com.casaverde.backend.dto.FondoDTO;
import com.casaverde.backend.dto.GastoDTO;
import com.casaverde.backend.dto.PedidoDTO;
import com.casaverde.backend.dto.PedidoDetalleDTO;
import com.casaverde.backend.dto.PagoDTO;
import com.casaverde.backend.dto.ResumenDiaDTO;
import com.casaverde.backend.repository.FondoRepository;
import com.casaverde.backend.repository.GastoRepository;
import com.casaverde.backend.repository.PagoRepository;
import com.casaverde.backend.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumenService {

    private final PedidoRepository pedidoRepo;
    private final PagoRepository pagoRepo;
    private final GastoRepository gastoRepo;
    private final FondoRepository fondoRepo;

    @Transactional(readOnly = true)
    public ResumenDiaDTO obtener(LocalDate fecha) {
        LocalDateTime ini = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();

        List<Pedido> pedidosCreados = pedidoRepo.findByCreadoEnBetweenOrderByCreadoEnDesc(ini, fin);
        List<Pedido> pedidosFinalizados = pedidoRepo.findByFinalizadoEnBetweenOrderByFinalizadoEnDesc(ini, fin);
        // Excluir pagos de pedidos ANULADOS — esos fondos ya fueron revertidos al anular
        // JOIN FETCH evita N+1 al acceder a pago.getPedido().getEstado()
        List<Pago>   pagos   = pagoRepo.findWithPedidoByRegistradoEnBetween(ini, fin)
                .stream()
                .filter(p -> p.getPedido() == null
                        || !EstadoPedido.ANULADO.name().equals(p.getPedido().getEstado()))
                .collect(Collectors.toList());
        List<Gasto>  gastos  = gastoRepo.findByRegistradoEnBetweenOrderByRegistradoEnDesc(ini, fin);
        List<Fondo>  fondos  = fondoRepo.findAll();

        // ---- cobros ----
        BigDecimal cobCOP   = sumarPagosPorMoneda(pagos, "COP");
        BigDecimal cobUSD   = sumarPagosPorMoneda(pagos, "USD");
        BigDecimal cobNEQUI = sumarPagosPorMoneda(pagos, "NEQUI");
        BigDecimal cobTotalCop = pagos.stream().map(Pago::getMontoCop).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---- gastos ----
        BigDecimal gCOP    = sumarGastosPorMoneda(gastos, "COP");
        BigDecimal gUSD    = sumarGastosPorMoneda(gastos, "USD");
        BigDecimal gNEQUI  = sumarGastosPorMoneda(gastos, "NEQUI");
        BigDecimal gTotalCop = gastos.stream().map(Gasto::getMontoCop).reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResumenDiaDTO.builder()
                .fecha(fecha.toString())
                // pedidos
                .pedidosTotales(pedidosCreados.size())
                .pedidosAbiertos(countActivos(pedidosCreados))
                .pedidosFinalizados(pedidosFinalizados.size())
                .pedidosAnulados(count(pedidosCreados, EstadoPedido.ANULADO.name()))
                // cobros
                .cobradoCop(cobCOP)
                .cobradoUsd(cobUSD)
                .cobradoNequi(cobNEQUI)
                .cobradoTotalCop(cobTotalCop)
                // gastos
                .gastosCop(gCOP)
                .gastosUsd(gUSD)
                .gastosNequi(gNEQUI)
                .gastosTotalCop(gTotalCop)
                // balance
                .balanceNeto(cobTotalCop.subtract(gTotalCop))
                // listas
                .fondos(fondos.stream().map(this::fondoToDTO).collect(Collectors.toList()))
                .pedidos(pedidosFinalizados.stream().map(this::pedidoToDTO).collect(Collectors.toList()))
                .gastos(gastos.stream().map(this::gastoToDTO).collect(Collectors.toList()))
                .build();
    }

    // ---- helpers ----

    private long count(List<Pedido> list, String estado) {
        return list.stream().filter(p -> estado.equals(p.getEstado())).count();
    }

    private long countActivos(List<Pedido> list) {
        return list.stream()
                .filter(p -> EstadoPedido.from(p.getEstado()).esActivo())
                .count();
    }

    private BigDecimal sumarPagosPorMoneda(List<Pago> pagos, String moneda) {
        return pagos.stream().filter(p -> moneda.equals(p.getMoneda()))
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarGastosPorMoneda(List<Gasto> gastos, String moneda) {
        return gastos.stream().filter(g -> moneda.equals(g.getMoneda()))
                .map(Gasto::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PedidoDTO pedidoToDTO(Pedido p) {
        BigDecimal total = p.getDetalles().stream()
                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonado = p.getPagos().stream().map(Pago::getMontoCop).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoPendiente = total.subtract(abonado).max(BigDecimal.ZERO);
        BigDecimal valorCambio = abonado.subtract(total).max(BigDecimal.ZERO);
        return PedidoDTO.builder()
                .id(p.getId())
                .salonId(p.getSalon() != null ? p.getSalon().getId() : null)
                .salonNombre(p.getSalon() != null ? p.getSalon().getNombre() : null)
                .mesa(p.getMesa())
                .estado(EstadoPedido.from(p.getEstado()).name())
                .totalCop(total)
                .totalAbonado(abonado)
                .saldoPendiente(saldoPendiente)
                .valorCambio(valorCambio)
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .finalizadoEn(p.getFinalizadoEn())
                .detalles(p.getDetalles().stream().map(d -> PedidoDetalleDTO.builder()
                        .id(d.getId())
                        .nombreSnapshot(d.getNombreSnapshot())
                        .precioUnitarioCop(d.getPrecioUnitarioCop())
                        .cantidad(d.getCantidad())
                        .subtotalCop(d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                        .build()).collect(Collectors.toList()))
                .pagos(p.getPagos().stream().map(pg -> PagoDTO.builder()
                        .id(pg.getId())
                        .moneda(pg.getMoneda())
                        .monto(pg.getMonto())
                        .montoCop(pg.getMontoCop())
                        .registradoEn(pg.getRegistradoEn())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private GastoDTO gastoToDTO(Gasto g) {
        return GastoDTO.builder()
                .id(g.getId()).concepto(g.getConcepto()).categoria(g.getCategoria())
                .moneda(g.getMoneda()).monto(g.getMonto()).montoCop(g.getMontoCop())
                .tasaUsada(g.getTasaUsada()).observacion(g.getObservacion())
                .registradoEn(g.getRegistradoEn()).build();
    }

    private FondoDTO fondoToDTO(Fondo f) {
        return FondoDTO.builder()
                .moneda(f.getMoneda()).saldo(f.getSaldo()).actualizadoEn(f.getActualizadoEn()).build();
    }
}
