package com.casaverde.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.HistorialAuditoriaRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoPrecioHistorialRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.TasaCambioRepository;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;
import com.casaverde.backend.fachadaServices.dto.CierreCajaRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.GastoRequest;
import com.casaverde.backend.fachadaServices.dto.LoginRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;
import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;
import com.casaverde.backend.fachadaServices.services.pedidoService.ICierreCajaService;
import com.casaverde.backend.fachadaServices.services.pedidoService.IPedidoService;
import com.casaverde.backend.fachadaServices.services.cajaService.IGastoService;
import com.casaverde.backend.fachadaServices.services.productoService.IProductoService;
import com.casaverde.backend.fachadaServices.services.productoService.IUsuarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CasaVerdeBusinessRulesTests {

        @Autowired
        private IPedidoService pedidoService;

        @Autowired
        private IGastoService gastoService;

        @Autowired
        private IFondoService fondoService;

        @Autowired
        private IUsuarioService usuarioService;

        @Autowired
        private ICierreCajaService cierreCajaService;

        @Autowired
        private IProductoService productoService;

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

        @Autowired
        private ProductoPrecioHistorialRepository precioHistorialRepository;

        @Autowired
        private com.casaverde.backend.fachadaServices.services.pedidoService.ICuentaCobrarService cuentaCobrarService;

        @Autowired
        private com.casaverde.backend.fachadaServices.services.compraService.ICompraService compraService;

        @Autowired
        private com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository proveedorRepository;

        @Test
        void noFinalizaPedidoSiNoHayStockSuficiente() {
                Inventario cuy = inventarioRepository.findByInsumo("cuy").orElseThrow();
                cuy.setStockActual(BigDecimal.ZERO);
                inventarioRepository.save(cuy);

                Producto productoCuy = productoRepository.findByEliminadoFalseOrderByNombreAsc().stream()
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
                ajustarFondo("COP", BigDecimal.valueOf(20000));
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
        void gastoSinSaldoDisponibleEsRechazado() {
                GastoRequest request = new GastoRequest();
                request.setConcepto("Gasto sin caja");
                request.setCategoria("otros");
                request.setMoneda("COP");
                request.setMonto(BigDecimal.valueOf(1000));

                assertThatThrownBy(() -> gastoService.registrar(request))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("no tiene saldo suficiente");
                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.ZERO);
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

                // El fondo solo se acredita al finalizar, no en el momento del pago
                pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));
                pedidoService.finalizarPedido(pedido.getId());

                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.valueOf(1000));
                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .extracting(m -> m.getTipo() + "|" + m.getReferenciaTipo() + "|" + m.getReferenciaId())
                                .contains(CajaMovimientoTipo.COBRO_PEDIDO.name() + "|pedido|" + pedido.getId());
                assertThat(historialAuditoriaRepository.findByEntidadAndEntidadIdOrderByRegistradoEnDesc("pedido",
                                pedido.getId()))
                                .anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("REGISTRAR_PAGO"));
        }

        @Test
        void anularPedidoReviertePagosEnLibroCajaYFondo() {
                PedidoDTO pedido = crearPedidoManual("Cliente reverso", BigDecimal.valueOf(1000));
                pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

                PedidoDTO anulado = pedidoService.anularPedido(pedido.getId());

                assertThat(anulado.getEstado()).isEqualTo(EstadoPedido.ANULADO.name());
                // El fondo solo se acredita al finalizar; anular antes de finalizar no genera
                // impacto en fondos
                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .noneMatch(m -> CajaMovimientoTipo.REVERSO_PAGO.name().equals(m.getTipo())
                                                || CajaMovimientoTipo.COBRO_PEDIDO.name().equals(m.getTipo()));
        }

        @Test
        void loginLocalSoloExigeClaveAlAdmin() {
                List<UsuarioDTO> usuarios = usuarioService.listarActivos();
                UsuarioDTO admin = usuarios.stream().filter(u -> "ADMIN".equals(u.getRol())).findFirst().orElseThrow();
                UsuarioDTO operador = usuarios.stream().filter(u -> "OPERADOR".equals(u.getRol())).findFirst()
                                .orElseThrow();

                assertThat(usuarioService.login(new LoginRequest(operador.getId(), null)).getNombre())
                                .isEqualTo(operador.getNombre());
                assertThatThrownBy(() -> usuarioService.login(new LoginRequest(admin.getId(), "mala")))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("incorrecta");
                assertThat(usuarioService.login(new LoginRequest(admin.getId(), "admin123")).getRol())
                                .isEqualTo("ADMIN");
        }

        @Test
        void cierreCajaBloqueaPedidosAbiertosYCreaBackupCuandoNoHayPendientes() {
                PedidoDTO pedido = crearPedidoManual("Cliente pendiente cierre", BigDecimal.valueOf(1000));
                CierreCajaRequest bloqueado = new CierreCajaRequest();
                bloqueado.setFecha(LocalDate.now());

                assertThatThrownBy(() -> cierreCajaService.cerrar(bloqueado))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("pedido(s) pendiente(s)");

                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));
                pedidoService.finalizarPedido(pedido.getId());

                CierreCajaRequest request = new CierreCajaRequest();
                request.setFecha(LocalDate.now());
                request.setContadoCop(BigDecimal.valueOf(1000));
                request.setContadoUsd(BigDecimal.ZERO);
                request.setContadoNequi(BigDecimal.ZERO);

                CierreCajaDTO cierre = cierreCajaService.cerrar(request);
                assertThat(cierre.getId()).isNotNull();
                assertThat(cierre.getBackupPath()).contains("casaverde-");
                assertThat(cierre.getDiferenciaCop()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void cambioDePrecioQuedaEnHistorial() {
                Producto producto = productoRepository.findByEliminadoFalseOrderByNombreAsc().stream()
                                .filter(p -> "Gaseosa".equals(p.getNombre()))
                                .findFirst()
                                .orElseThrow();

                productoService.actualizarPrecio(producto.getId(), BigDecimal.valueOf(3500), null);

                assertThat(precioHistorialRepository.findByProducto_IdOrderByRegistradoEnDesc(producto.getId()))
                                .anySatisfy(h -> {
                                        assertThat(h.getPrecioAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
                                        assertThat(h.getPrecioNuevo()).isEqualByComparingTo(BigDecimal.valueOf(3500));
                                        assertThat(h.getMotivo()).isEqualTo("ACTUALIZAR_PRECIO");
                                });
        }

        @Test
        void registrarAbonoCuentaCobrarConMontoNegativoEsRechazado() {
                ajustarFondo("COP", BigDecimal.valueOf(50000));

                var req = new com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest();
                req.setClienteNombre("Pedro Test");
                req.setMonto(BigDecimal.valueOf(30000));
                req.setFecha(LocalDate.now());
                var cuenta = cuentaCobrarService.registrar(req);

                var abonoReq = new com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest();
                abonoReq.setMonto(BigDecimal.valueOf(-5000));
                abonoReq.setMoneda("COP");
                abonoReq.setFecha(LocalDate.now());

                assertThatThrownBy(() -> cuentaCobrarService.registrarAbono(cuenta.getId(), abonoReq))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("mayor a 0");

                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.valueOf(50000));
        }

        @Test
        void registrarCompraConCantidadNegativaEsRechazada() {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Cantidad")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.GALLINA)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("gallina");
                req.setCantidad(BigDecimal.valueOf(-5));
                req.setPrecioUnitario(BigDecimal.valueOf(15000));
                req.setFecha(LocalDate.now());

                Inventario gallinaAntes = inventarioRepository.findByInsumo("gallina").orElseThrow();
                BigDecimal stockAntes = gallinaAntes.getStockActual();

                assertThatThrownBy(() -> compraService.registrar(req))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("mayor a 0");

                // El stock no debe haber cambiado
                assertThat(inventarioRepository.findByInsumo("gallina").orElseThrow().getStockActual())
                                .isEqualByComparingTo(stockAntes);
        }

        @Test
        void registrarCompraConPrecioNegativoEsRechazada() {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Precio")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.CUY)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("cuy");
                req.setCantidad(BigDecimal.valueOf(3));
                req.setPrecioUnitario(BigDecimal.valueOf(-1000));
                req.setFecha(LocalDate.now());

                assertThatThrownBy(() -> compraService.registrar(req))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("mayor a 0");
        }

        @Test
        void anularCompraConStockInsuficienteEsRechazada() {
                // Setup: create an active proveedor
                var proveedor = com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                .nombre("Proveedor Test Anulacion")
                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.GALLINA)
                                .activo(true)
                                .build();
                proveedor = proveedorRepository.save(proveedor);

                // Register a purchase of 5 gallinas (stock goes up by 5)
                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("gallina");
                req.setCantidad(BigDecimal.valueOf(5));
                req.setPrecioUnitario(BigDecimal.valueOf(30000));
                req.setFecha(LocalDate.now());
                var compra = compraService.registrar(req);

                // Manually reduce stock to 2 (simulate pedidos consuming stock between purchase
                // and cancellation)
                Inventario gallina = inventarioRepository.findByInsumo("gallina").orElseThrow();
                gallina.setStockActual(BigDecimal.valueOf(2));
                inventarioRepository.save(gallina);

                // Act & Assert: anular should fail, NOT silently corrupt inventory
                assertThatThrownBy(() -> compraService.anular(compra.getId()))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("Stock insuficiente");

                // Stock should remain at 2 (unchanged after the failed anulacion)
                assertThat(inventarioRepository.findByInsumo("gallina").orElseThrow().getStockActual())
                                .isEqualByComparingTo(BigDecimal.valueOf(2));
        }

        private PedidoDTO crearPedidoManual(String cliente, BigDecimal precioCop) {
                CrearPedidoRequest request = new CrearPedidoRequest();
                request.setNombre(cliente);
                request.setDetalles(List.of(new DetalleItemRequest(null, "Producto manual", precioCop, 1)));
                return pedidoService.crear(request);
        }

        private void ajustarFondo(String moneda, BigDecimal saldo) {
                AjusteCapitalRequest request = new AjusteCapitalRequest();
                request.setNuevoSaldo(saldo);
                request.setMotivo("Preparacion prueba");
                fondoService.ajustar(moneda, request);
        }
}
