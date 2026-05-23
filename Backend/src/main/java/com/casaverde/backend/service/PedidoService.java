package com.casaverde.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.*;
import com.casaverde.backend.dto.*;
import com.casaverde.backend.exception.*;
import com.casaverde.backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final SalonRepository salonRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final TasaCambioRepository tasaRepo;
    private final CajaMovimientoService cajaMovimientoService;
    private final MonetaryService monetaryService;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarActivos() {
        return pedidoRepo.findByEstadoInOrderByCreadoEnDesc(EstadoPedido.activos())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepo.findAllByOrderByCreadoEnDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional
    public PedidoDTO crear(CrearPedidoRequest req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            throw new ValidacionException("El nombre del cliente es obligatorio");

        Pedido pedido = Pedido.builder()
                .nombre(req.getNombre().trim())
                .mesa(req.getMesa())
                .fechaReserva(req.getFechaReserva())
                .horaReserva(req.getHoraReserva())
                .cantPersonas(req.getCantPersonas())
                .observacion(req.getObservacion())
                .estado(EstadoPedido.PENDIENTE.name())
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

        Pago pagoInicial = null;
        if (req.getAbono() != null && req.getAbono().compareTo(BigDecimal.ZERO) > 0) {
            String moneda = monetaryService.normalizarMoneda(req.getMonedaAbono() != null ? req.getMonedaAbono() : "COP");
            BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getAbono());
            BigDecimal tasa = "USD".equals(moneda) ? getTasaVigente() : null;
            BigDecimal montoCop = calcularMontoCop(moneda, monto);
            pagoInicial = Pago.builder()
                    .pedido(pedido)
                    .moneda(moneda)
                    .monto(monto)
                    .montoCop(montoCop)
                    .tasaUsada(tasa)
                    .build();
            pedido.getPagos().add(pagoInicial);
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pagoInicial != null ? pedidoRepo.saveAndFlush(pedido) : pedidoRepo.save(pedido);
        if (pagoInicial != null) {
            cajaMovimientoService.registrar(
                    CajaMovimientoTipo.COBRO_PEDIDO,
                    pagoInicial.getMoneda(),
                    pagoInicial.getMonto(),
                    pagoInicial.getMontoCop(),
                    "pago",
                    resolvePagoId(guardado, pagoInicial),
                    "Cobro pedido #" + guardado.getId(),
                    "Abono inicial");
        }
        auditoriaService.registrar("pedido", guardado.getId(), "CREAR",
                "Pedido creado para " + guardado.getNombre());
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO actualizarPedido(Long pedidoId, ActualizarPedidoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "actualizar");

        if (req.getNombre() != null && !req.getNombre().isBlank())
            pedido.setNombre(req.getNombre().trim());

        pedido.setFechaReserva(req.getFechaReserva());
        pedido.setHoraReserva(req.getHoraReserva());
        pedido.setMesa(req.getMesa());
        pedido.setCantPersonas(req.getCantPersonas());
        pedido.setObservacion(req.getObservacion());

        if (req.getSalonId() != null) {
            pedido.setSalon(salonRepo.findById(req.getSalonId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + req.getSalonId())));
        } else {
            pedido.setSalon(null);
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.saveAndFlush(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR",
                "Datos generales del pedido actualizados");
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO agregarDetalle(Long pedidoId, DetalleItemRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "agregar ítems");

        if (req.getCantidad() == null || req.getCantidad() < 1)
            throw new ValidacionException("La cantidad debe ser al menos 1");

        boolean merged = false;

        if (req.getProductoId() != null) {
            // Producto de catálogo: sumar si ya existe el mismo productoId
            for (PedidoDetalle d : pedido.getDetalles()) {
                if (d.getProducto() != null && d.getProducto().getId().equals(req.getProductoId())) {
                    d.setCantidad(d.getCantidad() + req.getCantidad());
                    merged = true;
                    break;
                }
            }
        } else {
            // Ítem manual: sumar si mismo nombre y precio
            String nombre = req.getNombreManual() != null ? req.getNombreManual().trim() : "";
            BigDecimal precio = monetaryService.normalizarPrecioCop(req.getPrecioManual());
            for (PedidoDetalle d : pedido.getDetalles()) {
                if (d.getProducto() == null
                        && nombre.equalsIgnoreCase(d.getNombreSnapshot())
                        && precio.compareTo(d.getPrecioUnitarioCop()) == 0) {
                    d.setCantidad(d.getCantidad() + req.getCantidad());
                    merged = true;
                    break;
                }
            }
        }

        if (!merged) {
            pedido.getDetalles().add(buildDetalle(pedido, req));
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.saveAndFlush(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "AGREGAR_ITEM",
                "Item agregado o acumulado en el pedido");
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO quitarDetalle(Long pedidoId, Long detalleId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "quitar ítems");
        boolean removed = pedido.getDetalles().removeIf(d -> d.getId().equals(detalleId));
        recalcularEstadoPago(pedido);
        if (!removed) throw new RecursoNoEncontradoException("Ítem no encontrado: " + detalleId);
        Pedido guardado = pedidoRepo.saveAndFlush(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "QUITAR_ITEM",
                "Item eliminado del pedido: " + detalleId);
        return toDTO(guardado);
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
        Pedido guardado = pedidoRepo.saveAndFlush(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_ITEM",
                "Item actualizado en el pedido: " + detalleId);
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO registrarPago(Long pedidoId, RegistrarPagoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        if (EstadoPedido.ANULADO.name().equals(pedido.getEstado()))
            throw new ValidacionException("No se puede registrar pago en un pedido ANULADO");
        if (EstadoPedido.FINALIZADO.name().equals(pedido.getEstado()))
            throw new ValidacionException("El pedido ya está FINALIZADO");
        if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto debe ser mayor a 0");

        String moneda = monetaryService.normalizarMoneda(req.getMoneda());
        BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getMonto());
        BigDecimal montoCop = calcularMontoCop(moneda, monto);
        BigDecimal tasaUsada = "USD".equals(moneda) ? getTasaVigente() : null;

        Pago pago = Pago.builder()
                .pedido(pedido)
                .moneda(moneda)
                .monto(monto)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .build();
        pedido.getPagos().add(pago);

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.saveAndFlush(pedido);
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.COBRO_PEDIDO,
                pago.getMoneda(),
                pago.getMonto(),
                pago.getMontoCop(),
                "pago",
                resolvePagoId(guardado, pago),
                "Cobro pedido #" + guardado.getId(),
                null);
        auditoriaService.registrar("pedido", guardado.getId(), "REGISTRAR_PAGO",
                "Pago " + moneda + " " + monto + " equivalente COP " + montoCop);
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO finalizarPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "finalizar");

        if (pedido.getDetalles().isEmpty())
            throw new ValidacionException("El pedido no tiene ítems");

        BigDecimal total = calcularTotal(pedido);
        BigDecimal abonado = calcularAbonado(pedido);
        recalcularEstadoPago(pedido);
        if (!EstadoPedido.PAGADO.name().equals(pedido.getEstado())) {
            throw new ValidacionException(String.format(
                    "Saldo pendiente: $%.0f COP. Registre el pago completo antes de finalizar.",
                    total.subtract(abonado)));
        }

        descontarInventario(pedido);
        pedido.setEstado(EstadoPedido.FINALIZADO.name());
        pedido.setFinalizadoEn(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "FINALIZAR",
                "Pedido finalizado con total COP " + total);
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO anularPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        if (EstadoPedido.ANULADO.name().equals(pedido.getEstado()))
            throw new ValidacionException("El pedido ya está ANULADO");
        if (EstadoPedido.FINALIZADO.name().equals(pedido.getEstado()))
            throw new ValidacionException("No se puede anular un pedido FINALIZADO");

        // Revertir todos los pagos registrados de los fondos correspondientes
        for (Pago pago : pedido.getPagos()) {
            cajaMovimientoService.registrar(
                    CajaMovimientoTipo.REVERSO_PAGO,
                    pago.getMoneda(),
                    pago.getMonto().negate(),
                    pago.getMontoCop().negate(),
                    "pago",
                    pago.getId(),
                    "Reverso por anulacion pedido #" + pedido.getId(),
                    null);
        }

        pedido.setEstado(EstadoPedido.ANULADO.name());
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ANULAR",
                "Pedido anulado. " + pedido.getPagos().size() + " pago(s) revertido(s) de fondos");
        return toDTO(guardado);
    }

    @Transactional
    public PedidoDTO quitarPago(Long pedidoId, Long pagoId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "eliminar cobros");

        Pago pago = pedido.getPagos().stream()
                .filter(p -> p.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Cobro no encontrado: " + pagoId));

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.REVERSO_PAGO,
                pago.getMoneda(),
                pago.getMonto().negate(),
                pago.getMontoCop().negate(),
                "pago",
                pago.getId(),
                "Reverso pago pedido #" + pedido.getId(),
                "Pago eliminado");
        pedido.getPagos().remove(pago);
        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "QUITAR_PAGO",
                "Pago eliminado: " + pagoId);
        return toDTO(guardado);
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

        String monedaAnterior = pago.getMoneda();
        BigDecimal montoAnterior = pago.getMonto();
        BigDecimal montoCopAnterior = pago.getMontoCop();

        // Calcular nuevos valores
        String nuevaMoneda = monetaryService.normalizarMoneda(req.getMoneda());
        BigDecimal nuevoMonto = monetaryService.normalizarMontoEntrada(nuevaMoneda, req.getMonto());
        BigDecimal nuevoMontoCop = calcularMontoCop(nuevaMoneda, nuevoMonto);
        BigDecimal nuevaTasa = "USD".equals(nuevaMoneda) ? getTasaVigente() : null;

        // Mutar la entidad (CASCADE persiste el cambio)
        pago.setMoneda(nuevaMoneda);
        pago.setMonto(nuevoMonto);
        pago.setMontoCop(nuevoMontoCop);
        pago.setTasaUsada(nuevaTasa);

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.saveAndFlush(pedido);
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.REVERSO_PAGO,
                monedaAnterior,
                montoAnterior.negate(),
                montoCopAnterior.negate(),
                "pago",
                pago.getId(),
                "Reverso por edicion pago pedido #" + guardado.getId(),
                null);
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.COBRO_PEDIDO,
                nuevaMoneda,
                nuevoMonto,
                nuevoMontoCop,
                "pago",
                pago.getId(),
                "Cobro editado pedido #" + guardado.getId(),
                null);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_PAGO",
                "Pago actualizado: " + pagoId + " a " + nuevaMoneda + " " + nuevoMonto);
        return toDTO(guardado);
    }

    // ---- private helpers ----

    private PedidoDetalle buildDetalle(Pedido pedido, DetalleItemRequest req) {
        if (req.getCantidad() == null || req.getCantidad() < 1)
            throw new ValidacionException("La cantidad debe ser al menos 1");

        PedidoDetalle detalle = new PedidoDetalle();
        detalle.setPedido(pedido);
        detalle.setCantidad(req.getCantidad());

        if (req.getProductoId() != null) {
            Producto prod = productoRepo.findById(req.getProductoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + req.getProductoId()));
            if (!Boolean.TRUE.equals(prod.getActivo()))
                throw new ValidacionException("El producto '" + prod.getNombre() + "' está inactivo y no puede agregarse a pedidos");
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
            return null;
        }
    }

    private void descontarInventario(Pedido pedido) {
        Map<String, BigDecimal> consumos = calcularConsumosInventario(pedido);
        validarStockDisponible(consumos);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarMovimientoInventario(consumo.getKey(), "salida", consumo.getValue(), pedido.getId());
        }
    }

    private void revertirInventario(Pedido pedido) {
        Map<String, BigDecimal> consumos = calcularConsumosInventario(pedido);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarMovimientoInventario(consumo.getKey(), "entrada", consumo.getValue(), pedido.getId());
        }
    }

    private Map<String, BigDecimal> calcularConsumosInventario(Pedido pedido) {
        Map<String, BigDecimal> consumos = new LinkedHashMap<>();
        for (PedidoDetalle detalle : pedido.getDetalles()) {
            if (detalle.getComposicionSnapshot() == null || detalle.getComposicionSnapshot().isBlank()) continue;
            for (Map<String, Object> item : leerComposicionSnapshot(detalle)) {
                Object insumoRaw = item.get("insumo");
                Object cantidadRaw = item.get("cantidad");
                if (!(insumoRaw instanceof String insumo) || insumo.isBlank() || !(cantidadRaw instanceof Number cantidad)) {
                    throw new ValidacionException("Composicion de inventario invalida en el item: " + detalle.getNombreSnapshot());
                }
                BigDecimal cantPorUnidad = new BigDecimal(cantidadRaw.toString()).setScale(4, RoundingMode.HALF_UP);
                if (cantPorUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidacionException("Cantidad de inventario invalida para " + insumo + " en el item: " + detalle.getNombreSnapshot());
                }
                consumos.merge(insumo.trim(), cantPorUnidad.multiply(BigDecimal.valueOf(detalle.getCantidad())), BigDecimal::add);
            }
        }
        return consumos;
    }

    private List<Map<String, Object>> leerComposicionSnapshot(PedidoDetalle detalle) {
        try {
            return objectMapper.readValue(detalle.getComposicionSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ValidacionException("No se pudo leer la composicion de inventario del item: " + detalle.getNombreSnapshot());
        }
    }

    private void validarStockDisponible(Map<String, BigDecimal> consumos) {
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            Inventario inv = inventarioRepo.findByInsumo(consumo.getKey())
                    .orElseThrow(() -> new ValidacionException("No existe inventario configurado para el insumo: " + consumo.getKey()));
            if (inv.getStockActual().compareTo(consumo.getValue()) < 0) {
                throw new ValidacionException("Stock insuficiente de " + consumo.getKey()
                        + ". Disponible: " + inv.getStockActual()
                        + ", requerido: " + consumo.getValue());
            }
        }
    }

    private void registrarMovimientoInventario(String insumo, String tipo, BigDecimal cantidad, Long pedidoId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("No existe inventario configurado para el insumo: " + insumo));
        if ("salida".equals(tipo) && inv.getStockActual().compareTo(cantidad) < 0) {
            throw new ValidacionException("Stock insuficiente de " + insumo
                    + ". Disponible: " + inv.getStockActual()
                    + ", requerido: " + cantidad);
        }
        BigDecimal nuevo = "entrada".equals(tipo)
                ? inv.getStockActual().add(cantidad)
                : inv.getStockActual().subtract(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);

        String obs = "entrada".equals(tipo)
                ? "Anulación pedido #" + pedidoId
                : "Finalización pedido #" + pedidoId;

        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo).tipo(tipo).cantidad(cantidad)
                .stockResultante(nuevo).referenciaTipo("pedido")
                .referenciaId(pedidoId).observacion(obs)
                .build());
    }

    private BigDecimal calcularMontoCop(String moneda, BigDecimal monto) {
        return monetaryService.convertirACop(moneda, monto, getTasaVigente());
    }

    private Long resolvePagoId(Pedido pedido, Pago pago) {
        if (pago.getId() != null) {
            return pago.getId();
        }
        return pedido.getPagos().stream()
                .map(Pago::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(null);
    }

    private BigDecimal getTasaVigente() {
        return tasaRepo.findVigente()
                .orElseThrow(() -> new ValidacionException("No hay tasa de cambio configurada"))
                .getValorCop();
    }

    private BigDecimal calcularTotal(Pedido pedido) {
        return pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularAbonado(Pedido pedido) {
        return pedido.getPagos().stream()
                .map(Pago::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recalcularEstadoPago(Pedido pedido) {
        EstadoPedido actual = EstadoPedido.from(pedido.getEstado());
        if (!actual.permiteEdicion()) return;

        BigDecimal total = calcularTotal(pedido);
        BigDecimal abonado = calcularAbonado(pedido);
        if (total.compareTo(BigDecimal.ZERO) <= 0 || abonado.compareTo(BigDecimal.ZERO) <= 0) {
            pedido.setEstado(EstadoPedido.PENDIENTE.name());
        } else if (abonado.compareTo(total) < 0) {
            pedido.setEstado(EstadoPedido.ABONADO_PARCIAL.name());
        } else {
            pedido.setEstado(EstadoPedido.PAGADO.name());
        }
    }

    private void validarAbierto(Pedido pedido, String accion) {
        if (!EstadoPedido.from(pedido.getEstado()).permiteEdicion())
            throw new ValidacionException("No se puede " + accion + " en un pedido " + pedido.getEstado());
    }

    private Pedido findOrThrow(Long id) {
        return pedidoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    private PedidoDTO toDTO(Pedido p) {
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
                .fechaReserva(p.getFechaReserva())
                .horaReserva(p.getHoraReserva())
                .cantPersonas(p.getCantPersonas())
                .estado(EstadoPedido.from(p.getEstado()).name())
                .observacion(p.getObservacion())
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

    private PedidoDetalleDTO toDetalleDTO(PedidoDetalle d) {
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

    private PagoDTO toPagoDTO(Pago p) {
        return PagoDTO.builder()
                .id(p.getId())
                .moneda(p.getMoneda())
                .monto(p.getMonto())
                .montoCop(p.getMontoCop())
                .tasaUsada(p.getTasaUsada())
                .registradoEn(p.getRegistradoEn())
                .build();
    }
}
