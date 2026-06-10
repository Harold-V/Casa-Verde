package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Gasto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.fachadaServices.dto.PedidoTotales;
import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;
import com.casaverde.backend.fachadaServices.mapper.FondoMapper;
import com.casaverde.backend.fachadaServices.mapper.GastoMapper;
import com.casaverde.backend.fachadaServices.mapper.PedidoMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.GastoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PagoRepository;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumenServiceImpl implements IResumenService {

    private final PedidoRepository pedidoRepo;
    private final PagoRepository pagoRepo;
    private final GastoRepository gastoRepo;
    private final FondoRepository fondoRepo;
    private final PedidoMapper pedidoMapper;
    private final GastoMapper gastoMapper;
    private final FondoMapper fondoMapper;

    @Transactional(readOnly = true)
    public ResumenDiaDTO obtener(LocalDate fecha) {
        LocalDateTime ini = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();

        List<Pedido> pedidosCreados = pedidoRepo.findByCreadoEnBetweenOrderByCreadoEnDesc(ini, fin);
        List<Pedido> pedidosFinalizados = pedidoRepo.findByFinalizadoEnBetweenOrderByFinalizadoEnDesc(ini, fin);
        // Excluir pagos de pedidos ANULADOS — esos fondos ya fueron revertidos al
        // anular
        // JOIN FETCH evita N+1 al acceder a pago.getPedido().getEstado()
        List<Pago> pagos = pagoRepo.findWithPedidoByRegistradoEnBetween(ini, fin)
                .stream()
                .filter(p -> p.getPedido() == null
                        || EstadoPedido.ANULADO != p.getPedido().getEstado())
                .collect(Collectors.toList());
        List<Gasto> gastos = gastoRepo.findByRegistradoEnBetweenOrderByRegistradoEnDesc(ini, fin);
        List<Fondo> fondos = fondoRepo.findAll();

        // ---- cobros ----
        BigDecimal cobCOP = sumarPagosPorMoneda(pagos, MonedaConstants.COP);
        BigDecimal cobUSD = sumarPagosPorMoneda(pagos, MonedaConstants.USD);
        BigDecimal cobNEQUI = sumarPagosPorMoneda(pagos, MonedaConstants.NEQUI);
        BigDecimal cobTotalCop = pagos.stream().map(Pago::getMontoCop).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---- gastos ----
        BigDecimal gCOP = sumarGastosPorMoneda(gastos, MonedaConstants.COP);
        BigDecimal gUSD = sumarGastosPorMoneda(gastos, MonedaConstants.USD);
        BigDecimal gNEQUI = sumarGastosPorMoneda(gastos, MonedaConstants.NEQUI);
        BigDecimal gTotalCop = gastos.stream().map(Gasto::getMontoCop).reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResumenDiaDTO.builder()
                .fecha(fecha.toString())
                // pedidos
                .pedidosTotales(pedidosCreados.size())
                .pedidosAbiertos(countActivos(pedidosCreados))
                .pedidosFinalizados(pedidosFinalizados.size())
                .pedidosAnulados(count(pedidosCreados, EstadoPedido.ANULADO))
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
                .fondos(fondos.stream().map(fondoMapper::toDTO).collect(Collectors.toList()))
                .pedidos(Stream.concat(
                        pedidosFinalizados.stream(),
                        pedidosCreados.stream().filter(p -> EstadoPedido.ANULADO == p.getEstado()))
                        .map(p -> pedidoMapper.toDTO(p, calcularTotales(p))).collect(Collectors.toList()))
                .gastos(gastos.stream().map(gastoMapper::toDTO).collect(Collectors.toList()))
                .build();
    }

    // ---- helpers ----

    private long count(List<Pedido> list, EstadoPedido estado) {
        return list.stream().filter(p -> estado == p.getEstado()).count();
    }

    private long countActivos(List<Pedido> list) {
        return list.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().esActivo())
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

    private PedidoTotales calcularTotales(Pedido pedido) {
        return PedidoTotales.of(pedido);
    }

}
