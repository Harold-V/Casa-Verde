package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.fachadaServices.dto.ActualizarDetalleRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoTotales;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.mapper.PedidoMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements IPedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final SalonRepository salonRepo;
    private final IComprobanteService comprobanteService;
    private final ITasaCambioService tasaCambioService;
    private final IFondoService fondoService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final IPedidoFinalizacionService finalizacionService;
    private final ObjectMapper objectMapper;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarActivos() {
        return pedidoRepo.findByEstadoInOrderByCreadoEnDesc(EstadoPedido.activos())
                .stream().map(p -> pedidoMapper.toDTO(p, calcularTotales(p))).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepo.findAllByOrderByCreadoEnDesc()
                .stream().map(p -> pedidoMapper.toDTO(p, calcularTotales(p))).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PedidoDTO> listarTodosPaginado(Pageable pageable) {
        return pedidoRepo.findAllByOrderByCreadoEnDesc(pageable)
                .map(p -> pedidoMapper.toDTO(p, calcularTotales(p)));
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long id) {
        Pedido pedido = findOrThrow(id);
        return pedidoMapper.toDTO(pedido, calcularTotales(pedido));
    }

    @Transactional(readOnly = true)
    public String comprobanteTexto(Long id) {
        Pedido pedido = findOrThrow(id);
        return comprobanteService.generar(pedidoMapper.toDTO(pedido, calcularTotales(pedido)));
    }

    @Transactional
    public PedidoDTO crear(CrearPedidoRequest req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            throw new ValidacionException("El nombre del cliente es obligatorio");

        boolean tieneFecha = req.getFechaReserva() != null && !req.getFechaReserva().isBlank();
        boolean tieneHora  = req.getHoraReserva()  != null && !req.getHoraReserva().isBlank();
        if (tieneFecha || tieneHora) {
            ZonedDateTime now   = ZonedDateTime.now(ZoneId.of("America/Bogota"));
            LocalDate     today = now.toLocalDate();
            LocalDate rFecha = tieneFecha ? LocalDate.parse(req.getFechaReserva()) : today;
            if (rFecha.isBefore(today))
                throw new ValidacionException("La fecha de reserva ya pasó");
            if (rFecha.isEqual(today) && tieneHora) {
                LocalTime rHora = LocalTime.parse(req.getHoraReserva());
                if (!rHora.isAfter(now.toLocalTime()))
                    throw new ValidacionException("La hora de reserva ya pasó");
            }
        }

        log.info("[crear] nombre='{}' detalles={}", req.getNombre(), req.getDetalles() != null ? req.getDetalles().size() : 0);

        Pedido pedido = Pedido.builder()
                .nombre(req.getNombre().trim())
                .mesa(req.getMesa())
                .fechaReserva(req.getFechaReserva() != null && !req.getFechaReserva().isBlank()
                        ? LocalDate.parse(req.getFechaReserva())
                        : null)
                .horaReserva(req.getHoraReserva() != null && !req.getHoraReserva().isBlank()
                        ? LocalTime.parse(req.getHoraReserva())
                        : null)
                .cantPersonas(req.getCantPersonas())
                .observacion(req.getObservacion())
                .paraLlevar(Boolean.TRUE.equals(req.getParaLlevar()))
                .estado(EstadoPedido.PENDIENTE)
                .build();

        if (req.getSalonId() != null) {
            pedido.setSalon(salonRepo.findById(req.getSalonId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + req.getSalonId())));
        }

        if (req.getDetalles() != null) {
            for (DetalleItemRequest d : req.getDetalles()) {
                pedido.getDetalles().add(buildDetalle(pedido, d));
            }
        }

        pedidoRepo.save(pedido);

        if (req.getAbono() != null && req.getAbono().compareTo(BigDecimal.ZERO) > 0) {
            String moneda = monetaryService
                    .normalizarMoneda(resolverMoneda(req.getMonedaAbono() != null ? req.getMonedaAbono() : MonedaConstants.COP));
            BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getAbono());
            BigDecimal tasa = MonedaConstants.USD.equals(moneda) ? getTasaVigente() : null;
            BigDecimal montoCop = monetaryService.convertirACop(moneda, monto, tasa);
            // El abono queda registrado en el pedido; el fondo solo se acredita al
            // finalizar
            pedido.getPagos().add(Pago.builder()
                    .pedido(pedido).moneda(moneda).monto(monto)
                    .montoCop(montoCop).tasaUsada(tasa).build());
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "CREAR",
                "Pedido creado para " + guardado.getNombre());
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO actualizarPedido(Long pedidoId, ActualizarPedidoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "actualizar");

        if (req.getNombre() != null && !req.getNombre().isBlank())
            pedido.setNombre(req.getNombre().trim());

        pedido.setFechaReserva(req.getFechaReserva() != null && !req.getFechaReserva().isBlank()
                ? LocalDate.parse(req.getFechaReserva())
                : null);
        pedido.setHoraReserva(req.getHoraReserva() != null && !req.getHoraReserva().isBlank()
                ? LocalTime.parse(req.getHoraReserva())
                : null);
        pedido.setMesa(req.getMesa());
        pedido.setCantPersonas(req.getCantPersonas());
        pedido.setObservacion(req.getObservacion());
        // Solo actualizar paraLlevar si viene explícito; null = no tocar
        if (req.getParaLlevar() != null) {
            pedido.setParaLlevar(req.getParaLlevar());
        }

        if (req.getSalonId() != null) {
            pedido.setSalon(salonRepo.findById(req.getSalonId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + req.getSalonId())));
        } else {
            pedido.setSalon(null);
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR",
                "Datos generales del pedido actualizados");
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO agregarDetalle(Long pedidoId, DetalleItemRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "agregar ítems");

        if (req.getCantidad() == null || req.getCantidad() < 1)
            throw new ValidacionException("La cantidad debe ser al menos 1");

        Optional<PedidoDetalle> existente = buscarDetalleExistente(pedido, req);
        if (existente.isPresent()) {
            existente.get().setCantidad(existente.get().getCantidad() + req.getCantidad());
        } else {
            pedido.getDetalles().add(buildDetalle(pedido, req));
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "AGREGAR_ITEM",
                "Item agregado o acumulado en el pedido");
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO quitarDetalle(Long pedidoId, Long detalleId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "quitar ítems");
        boolean removed = pedido.getDetalles().removeIf(d -> d.getId().equals(detalleId));
        recalcularEstadoPago(pedido);
        if (!removed)
            throw new RecursoNoEncontradoException("Ítem no encontrado: " + detalleId);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "QUITAR_ITEM",
                "Item eliminado del pedido: " + detalleId);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO actualizarDetalle(Long pedidoId, Long detalleId, ActualizarDetalleRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "editar ítems");
        if (req.getCantidad() == null || req.getCantidad() < 0)
            throw new ValidacionException("La cantidad no puede ser negativa");

        PedidoDetalle detalle = pedido.getDetalles().stream()
                .filter(d -> d.getId().equals(detalleId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado: " + detalleId));

        if (req.getCantidad() == 0) {
            pedido.getDetalles().remove(detalle);
        } else {
            detalle.setCantidad(req.getCantidad());
        }
        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_ITEM",
                "Item actualizado en el pedido: " + detalleId);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO registrarPago(Long pedidoId, RegistrarPagoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        if (EstadoPedido.ANULADO == pedido.getEstado())
            throw new ValidacionException("No se puede registrar pago en un pedido ANULADO");
        if (EstadoPedido.FINALIZADO == pedido.getEstado())
            throw new ValidacionException("El pedido ya está FINALIZADO");
        if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto debe ser mayor a 0");

        String moneda = monetaryService.normalizarMoneda(resolverMoneda(req.getMoneda()));
        BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getMonto());
        BigDecimal tasaUsada = MonedaConstants.USD.equals(moneda) ? getTasaVigente() : null;
        BigDecimal montoCop = monetaryService.convertirACop(moneda, monto, tasaUsada);

        // El cobro queda en el pedido; el fondo solo se acredita al finalizar
        Pago pago = Pago.builder()
                .pedido(pedido).moneda(moneda).monto(monto)
                .montoCop(montoCop).tasaUsada(tasaUsada).build();
        pedido.getPagos().add(pago);

        recalcularEstadoPago(pedido);
        log.info("[registrarPago] pedidoId={} moneda={} montoCop={}", pedidoId, moneda, montoCop);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "REGISTRAR_PAGO",
                "Pago " + moneda + " " + monto + " equivalente COP " + montoCop);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO finalizarPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        log.info("[finalizar] pedidoId={}", pedidoId);
        validarAbierto(pedido, "finalizar");

        if (pedido.getDetalles().isEmpty())
            throw new ValidacionException("El pedido no tiene ítems");

        PedidoTotales totalesFinalizacion = calcularTotales(pedido);
        BigDecimal total = totalesFinalizacion.totalCop();
        BigDecimal abonado = totalesFinalizacion.totalAbonado();
        recalcularEstadoPago(pedido);
        if (EstadoPedido.PAGADO != pedido.getEstado()) {
            throw new ValidacionException(String.format(
                    "Saldo pendiente: $%.0f COP. Registre el pago completo antes de finalizar.",
                    total.subtract(abonado)));
        }

        // Validar que la hora de la reserva ya haya llegado
        if (pedido.getFechaReserva() != null || pedido.getHoraReserva() != null) {
            java.time.ZoneId bogota = java.time.ZoneId.of("America/Bogota");
            LocalDate hoy = LocalDate.now(bogota);
            LocalDate fechaRes = pedido.getFechaReserva() != null ? pedido.getFechaReserva() : hoy;
            boolean reservaFutura;
            if (fechaRes.isAfter(hoy)) {
                reservaFutura = true;
            } else if (fechaRes.isBefore(hoy)) {
                reservaFutura = false;
            } else { // misma fecha: revisar hora
                reservaFutura = pedido.getHoraReserva() != null &&
                        pedido.getHoraReserva().isAfter(LocalTime.now(bogota));
            }
            if (reservaFutura) {
                String cuando = (pedido.getFechaReserva() != null && pedido.getHoraReserva() != null)
                        ? pedido.getFechaReserva() + " a las " + pedido.getHoraReserva()
                        : pedido.getFechaReserva() != null ? pedido.getFechaReserva().toString()
                                : "las " + pedido.getHoraReserva();
                throw new ValidacionException(
                        "No se puede finalizar antes de la reserva (" + cuando + ")");
            }
        }

        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setFinalizadoEn(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        Pedido guardado = pedidoRepo.save(pedido);

        finalizacionService.ejecutar(guardado, total);

        log.info("[finalizar] pedidoId={} COMPLETADO", pedidoId);
        auditoriaService.registrar("pedido", guardado.getId(), "FINALIZAR",
                "Pedido finalizado con total COP " + total);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO anularPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        log.info("[anular] pedidoId={}", pedidoId);
        if (EstadoPedido.ANULADO == pedido.getEstado())
            throw new ValidacionException("El pedido ya está ANULADO");
        if (EstadoPedido.FINALIZADO == pedido.getEstado())
            throw new ValidacionException("No se puede anular un pedido FINALIZADO");

        // Los pagos previos nunca tocaron el fondo (el fondo solo se acredita al
        // finalizar),
        // por lo que no hay nada que revertir — solo cambiar el estado.
        pedido.setEstado(EstadoPedido.ANULADO);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ANULAR",
                "Pedido anulado con " + pedido.getPagos().size() + " pago(s) sin impacto en fondos");
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO quitarPago(Long pedidoId, Long pagoId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "eliminar cobros");

        Pago pago = pedido.getPagos().stream()
                .filter(p -> p.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Cobro no encontrado: " + pagoId));

        // El pago nunca ingresó al fondo; simplemente se elimina del pedido
        pedido.getPagos().remove(pago);
        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "QUITAR_PAGO",
                "Pago eliminado: " + pagoId);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO actualizarPago(Long pedidoId, Long pagoId, ActualizarPagoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "editar pagos");
        if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto debe ser mayor a 0");

        Pago pago = pedido.getPagos().stream()
                .filter(p -> p.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + pagoId));

        // Calcular nuevos valores (sin movimiento de fondo — solo se acredita al
        // finalizar)
        String nuevaMoneda = monetaryService.normalizarMoneda(resolverMoneda(req.getMoneda()));
        BigDecimal nuevoMonto = monetaryService.normalizarMontoEntrada(nuevaMoneda, req.getMonto());
        BigDecimal nuevaTasa = MonedaConstants.USD.equals(nuevaMoneda) ? getTasaVigente() : null;
        BigDecimal nuevoMontoCop = monetaryService.convertirACop(nuevaMoneda, nuevoMonto, nuevaTasa);

        pago.setMoneda(nuevaMoneda);
        pago.setMonto(nuevoMonto);
        pago.setMontoCop(nuevoMontoCop);
        pago.setTasaUsada(nuevaTasa);

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_PAGO",
                "Pago actualizado: " + pagoId + " a " + nuevaMoneda + " " + nuevoMonto);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    // ---- private helpers ----

    private Optional<PedidoDetalle> buscarDetalleExistente(Pedido pedido, DetalleItemRequest req) {
        if (req.getProductoId() != null) {
            return pedido.getDetalles().stream()
                    .filter(d -> d.getProducto() != null
                            && d.getProducto().getId().equals(req.getProductoId()))
                    .findFirst();
        }
        String nombre = req.getNombreManual() != null ? req.getNombreManual().trim() : "";
        BigDecimal precio = monetaryService.normalizarPrecioCop(req.getPrecioManual());
        return pedido.getDetalles().stream()
                .filter(d -> d.getProducto() == null
                        && nombre.equalsIgnoreCase(d.getNombreSnapshot())
                        && precio.compareTo(d.getPrecioUnitarioCop()) == 0)
                .findFirst();
    }

    private PedidoDetalle buildDetalle(Pedido pedido, DetalleItemRequest req) {
        if (req.getCantidad() == null || req.getCantidad() < 1)
            throw new ValidacionException("La cantidad debe ser al menos 1");

        PedidoDetalle detalle = new PedidoDetalle();
        detalle.setPedido(pedido);
        detalle.setCantidad(req.getCantidad());

        if (req.getProductoId() != null) {
            Producto prod = productoRepo.findById(req.getProductoId())
                    .orElseThrow(
                            () -> new RecursoNoEncontradoException("Producto no encontrado: " + req.getProductoId()));
            if (!Boolean.TRUE.equals(prod.getActivo()))
                throw new ValidacionException(
                        "El producto '" + prod.getNombre() + "' está inactivo y no puede agregarse a pedidos");
            detalle.setProducto(prod);
            detalle.setNombreSnapshot(prod.getNombre());
            detalle.setPrecioUnitarioCop(prod.getPrecio());
            if (!prod.getComposicion().isEmpty()) {
                detalle.setComposicionSnapshot(serializarComposicion(prod.getComposicion()));
            }
        } else {
            if (req.getNombreManual() == null || req.getNombreManual().isBlank())
                throw new ValidacionException("El nombre del ítem manual es obligatorio");
            detalle.setNombreSnapshot(req.getNombreManual().trim());
            detalle.setPrecioUnitarioCop(monetaryService.normalizarPrecioCop(req.getPrecioManual()));
        }

        return detalle;
    }

    private String serializarComposicion(List<ProductoComposicionInventario> composicion) {
        try {
            List<Map<String, Object>> list = composicion.stream()
                    .map(c -> Map.<String, Object>of("insumo", c.getInsumo(), "cantidad", c.getCantidad()))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new ValidacionException("Error serializando composición de inventario: " + e.getMessage());
        }
    }

    /**
     * Si la moneda es exactamente "NEQUI" la resuelve a la cuenta activa
     * configurada
     * (puede ser NEQUI_2, NEQUI_3, etc.). Cualquier otra moneda se devuelve sin
     * cambio.
     */
    private String resolverMoneda(String moneda) {
        if (moneda == null)
            return MonedaConstants.COP;
        return MonedaConstants.NEQUI.equalsIgnoreCase(moneda.trim()) ? fondoService.getCuentaNequiActiva() : moneda.trim();
    }

    private BigDecimal getTasaVigente() {
        return tasaCambioService.getValorVigente();
    }

    private void recalcularEstadoPago(Pedido pedido) {
        if (!pedido.getEstado().permiteEdicion()) return;
        PedidoTotales t = calcularTotales(pedido);
        pedido.setEstado(EstadoPedido.calcular(t.totalCop(), t.totalAbonado()));
    }

    private void validarAbierto(Pedido pedido, String accion) {
        if (!pedido.getEstado().permiteEdicion()) {
            log.warn("[validarAbierto] pedidoId={} accion='{}' estado={}", pedido.getId(), accion, pedido.getEstado());
            throw new ValidacionException("No se puede " + accion + " en un pedido " + pedido.getEstado());
        }
    }

    private PedidoTotales calcularTotales(Pedido pedido) {
        return PedidoTotales.of(pedido);
    }

    private Pedido findOrThrow(Long id) {
        return pedidoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

}
