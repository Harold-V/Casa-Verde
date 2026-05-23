package com.casaverde.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.Inventario;
import com.casaverde.backend.domain.CajaMovimientoTipo;
import com.casaverde.backend.domain.EstadoPedido;
import com.casaverde.backend.domain.Producto;
import com.casaverde.backend.dto.CrearPedidoRequest;
import com.casaverde.backend.dto.DetalleItemRequest;
import com.casaverde.backend.dto.GastoRequest;
import com.casaverde.backend.dto.RegistrarPagoRequest;
import com.casaverde.backend.dto.PedidoDTO;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.CajaMovimientoRepository;
import com.casaverde.backend.repository.FondoRepository;
import com.casaverde.backend.repository.HistorialAuditoriaRepository;
import com.casaverde.backend.repository.InventarioRepository;
import com.casaverde.backend.repository.ProductoRepository;
import com.casaverde.backend.repository.TasaCambioRepository;
import com.casaverde.backend.service.GastoService;
import com.casaverde.backend.service.PedidoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CasaVerdeBusinessRulesTests {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private GastoService gastoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private TasaCambioRepository tasaCambioRepository;

    @Autowired
    private CajaMovimientoRepository cajaMovimientoRepository;

    @Autowired
    private FondoRepository fondoRepository;

    @Autowired
    private HistorialAuditoriaRepository historialAuditoriaRepository;

    @Test
    void noFinalizaPedidoSiNoHayStockSuficiente() {
        Inventario cuy = inventarioRepository.findByInsumo("cuy").orElseThrow();
        cuy.setStockActual(BigDecimal.ZERO);
        inventarioRepository.save(cuy);

        Producto productoCuy = productoRepository.findAllByOrderByNombreAsc().stream()
                .filter(p -> "Cuy".equals(p.getNombre()))
                .findFirst()
                .orElseThrow();
        productoCuy.setPrecio(BigDecimal.valueOf(1000));
        productoRepository.save(productoCuy);

        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setNombre("Cliente prueba stock");
        request.setDetalles(List.of(new DetalleItemRequest(productoCuy.getId(), null, null, 1)));

        PedidoDTO pedido = pedidoService.crear(request);
        pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

        assertThatThrownBy(() -> pedidoService.finalizarPedido(pedido.getId()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Stock insuficiente de cuy");
    }

    @Test
    void gastoInventariableAumentaStockYDejaMovimiento() {
        Inventario gallina = inventarioRepository.findByInsumo("gallina").orElseThrow();
        BigDecimal stockInicial = gallina.getStockActual();

        GastoRequest request = new GastoRequest();
        request.setConcepto("Compra prueba gallinas");
        request.setCategoria("insumos");
        request.setMoneda("COP");
        request.setMonto(BigDecimal.valueOf(10000));
        request.setInsumoInventario("gallina");
        request.setCantidadInventario(new BigDecimal("3"));

        gastoService.registrar(request);

        Inventario actualizado = inventarioRepository.findByInsumo("gallina").orElseThrow();
        assertThat(actualizado.getStockActual()).isEqualByComparingTo(stockInicial.add(new BigDecimal("3")));
        assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                .anySatisfy(m -> {
                    assertThat(m.getTipo()).isEqualTo(CajaMovimientoTipo.GASTO.name());
                    assertThat(m.getMonto()).isEqualByComparingTo(BigDecimal.valueOf(-10000));
                });
    }

    @Test
    void pagoUsdAceptaCentavosYCalculaEquivalenteCop() {
        PedidoDTO pedido = crearPedidoManual("Cliente USD", BigDecimal.valueOf(5000));
        BigDecimal tasa = tasaCambioRepository.findVigente().orElseThrow().getValorCop();
        BigDecimal montoUsd = new BigDecimal("0.25");

        PedidoDTO actualizado = pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("USD", montoUsd));

        assertThat(actualizado.getPagos()).hasSize(1);
        assertThat(actualizado.getPagos().get(0).getMonto()).isEqualByComparingTo("0.25");
        assertThat(actualizado.getPagos().get(0).getMontoCop())
                .isEqualByComparingTo(montoUsd.multiply(tasa).setScale(0, RoundingMode.HALF_UP));
    }

    @Test
    void pagoCopRechazaCentavos() {
        PedidoDTO pedido = crearPedidoManual("Cliente COP decimal", BigDecimal.valueOf(5000));

        assertThatThrownBy(() -> pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("COP", new BigDecimal("0.25"))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("COP no debe tener centavos");
    }

    @Test
    void pedidoTransicionaPorEstadosDePagoHastaFinalizar() {
        PedidoDTO pedido = crearPedidoManual("Cliente estados", BigDecimal.valueOf(1000));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE.name());

        PedidoDTO parcial = pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("COP", BigDecimal.valueOf(250)));
        assertThat(parcial.getEstado()).isEqualTo(EstadoPedido.ABONADO_PARCIAL.name());

        PedidoDTO pagado = pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("COP", BigDecimal.valueOf(750)));
        assertThat(pagado.getEstado()).isEqualTo(EstadoPedido.PAGADO.name());

        PedidoDTO finalizado = pedidoService.finalizarPedido(pedido.getId());
        assertThat(finalizado.getEstado()).isEqualTo(EstadoPedido.FINALIZADO.name());
        assertThat(finalizado.getFinalizadoEn()).isNotNull();
    }

    @Test
    void pagoConSobranteSeparaSaldoPendienteYValorCambio() {
        PedidoDTO pedido = crearPedidoManual("Cliente cambio", BigDecimal.valueOf(1000));

        PedidoDTO pagado = pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1200)));

        assertThat(pagado.getEstado()).isEqualTo(EstadoPedido.PAGADO.name());
        assertThat(pagado.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pagado.getValorCambio()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void pagoRegistraLibroCajaAuditoriaYActualizaFondo() {
        PedidoDTO pedido = crearPedidoManual("Cliente caja", BigDecimal.valueOf(1000));

        PedidoDTO pagado = pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

        Long pagoId = pagado.getPagos().get(0).getId();
        assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                .isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                .extracting(m -> m.getTipo() + "|" + m.getReferenciaTipo() + "|" + m.getReferenciaId())
                .contains(CajaMovimientoTipo.COBRO_PEDIDO.name() + "|pago|" + pagoId);
        assertThat(historialAuditoriaRepository.findByEntidadAndEntidadIdOrderByRegistradoEnDesc("pedido", pedido.getId()))
                .anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("REGISTRAR_PAGO"));
    }

    @Test
    void anularPedidoReviertePagosEnLibroCajaYFondo() {
        PedidoDTO pedido = crearPedidoManual("Cliente reverso", BigDecimal.valueOf(1000));
        PedidoDTO pagado = pedidoService.registrarPago(
                pedido.getId(),
                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

        PedidoDTO anulado = pedidoService.anularPedido(pedido.getId());

        assertThat(anulado.getEstado()).isEqualTo(EstadoPedido.ANULADO.name());
        assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cajaMovimientoRepository.existsByReferenciaTipoAndReferenciaIdAndTipo(
                "pago", pagado.getPagos().get(0).getId(), CajaMovimientoTipo.REVERSO_PAGO.name())).isTrue();
    }

    private PedidoDTO crearPedidoManual(String cliente, BigDecimal precioCop) {
        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setNombre(cliente);
        request.setDetalles(List.of(new DetalleItemRequest(null, "Producto manual", precioCop, 1)));
        return pedidoService.crear(request);
    }
}
