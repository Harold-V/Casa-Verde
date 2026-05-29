package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.pagoService.ICajaMovimientoService;
import com.casaverde.backend.fachadaServices.services.pagoService.IFondoService;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoMovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDetalleDTO;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.mapper.PedidoMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements IPedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final SalonRepository salonRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final ITasaCambioService tasaCambioService;
    private final ICajaMovimientoService cajaMovimientoService;
    private final IFondoService fondoService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarActivos() {
        return pedidoRepo.findByEstadoInOrderByCreadoEnDesc(EstadoPedido.activos())
                .stream().map(pedidoMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepo.findAllByOrderByCreadoEnDesc()
                .stream().map(pedidoMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long id) {
        return pedidoMapper.toDTO(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public String comprobanteTexto(Long id) {
        Pedido pedido = findOrThrow(id);
        PedidoDTO dto = pedidoMapper.toDTO(pedido);
        StringBuilder sb = new StringBuilder();
        sb.append("ASADERO CASA VERDE").append(System.lineSeparator());
        sb.append("Comprobante pedido #").append(dto.getId()).append(System.lineSeparator());
        sb.append("--------------------------------").append(System.lineSeparator());
        sb.append("Cliente: ").append(dto.getNombre()).append(System.lineSeparator());
        sb.append("Estado: ").append(dto.getEstado()).append(System.lineSeparator());
        if (dto.getSalonNombre() != null || dto.getMesa() != null) {
            sb.append("Ubicacion: ")
                    .append(dto.getSalonNombre() != null ? dto.getSalonNombre() : "")
                    .append(dto.getMesa() != null ? " / Mesa " + dto.getMesa() : "")
                    .append(System.lineSeparator());
        }
        if (dto.getFinalizadoEn() != null) {
            sb.append("Cierre: ").append(dto.getFinalizadoEn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append(System.lineSeparator());
        }
        sb.append("--------------------------------").append(System.lineSeparator());
        for (PedidoDetalleDTO detalle : dto.getDetalles()) {
            sb.append(detalle.getCantidad()).append(" x ")
                    .append(detalle.getNombreSnapshot())
                    .append("  COP ").append(detalle.getSubtotalCop())
                    .append(System.lineSeparator());
        }
        sb.append("--------------------------------").append(System.lineSeparator());
        sb.append("Total COP: ").append(dto.getTotalCop()).append(System.lineSeparator());
        sb.append("Abonado COP: ").append(dto.getTotalAbonado()).append(System.lineSeparator());
        if (dto.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Saldo pendiente COP: ").append(dto.getSaldoPendiente()).append(System.lineSeparator());
        }
        if (dto.getValorCambio().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Cambio COP: ").append(dto.getValorCambio()).append(System.lineSeparator());
        }
        if (!dto.getPagos().isEmpty()) {
            sb.append("--------------------------------").append(System.lineSeparator());
            sb.append("Pagos").append(System.lineSeparator());
            for (PagoDTO pago : dto.getPagos()) {
                sb.append(pago.getMoneda()).append(" ")
                        .append(pago.getMonto())
                        .append(" = COP ").append(pago.getMontoCop());
                if (pago.getTasaUsada() != null) {
                    sb.append(" tasa ").append(pago.getTasaUsada());
                }
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
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
                    .normalizarMoneda(resolverMoneda(req.getMonedaAbono() != null ? req.getMonedaAbono() : "COP"));
            BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getAbono());
            BigDecimal tasa = "USD".equals(moneda) ? getTasaVigente() : null;
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
        return pedidoMapper.toDTO(guardado);
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
        return pedidoMapper.toDTO(guardado);
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
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "AGREGAR_ITEM",
                "Item agregado o acumulado en el pedido");
        return pedidoMapper.toDTO(guardado);
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
        return pedidoMapper.toDTO(guardado);
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
        return pedidoMapper.toDTO(guardado);
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
        BigDecimal tasaUsada = "USD".equals(moneda) ? getTasaVigente() : null;
        BigDecimal montoCop = monetaryService.convertirACop(moneda, monto, tasaUsada);

        // El cobro queda en el pedido; el fondo solo se acredita al finalizar
        Pago pago = Pago.builder()
                .pedido(pedido).moneda(moneda).monto(monto)
                .montoCop(montoCop).tasaUsada(tasaUsada).build();
        pedido.getPagos().add(pago);

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "REGISTRAR_PAGO",
                "Pago " + moneda + " " + monto + " equivalente COP " + montoCop);
        return pedidoMapper.toDTO(guardado);
    }

    @Transactional
    public PedidoDTO finalizarPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "finalizar");

        if (pedido.getDetalles().isEmpty())
            throw new ValidacionException("El pedido no tiene ítems");

        BigDecimal total = pedidoMapper.calcularTotal(pedido);
        BigDecimal abonado = pedidoMapper.calcularAbonado(pedido);
        recalcularEstadoPago(pedido);
        if (EstadoPedido.PAGADO != pedido.getEstado()) {
            throw new ValidacionException(String.format(
                    "Saldo pendiente: $%.0f COP. Registre el pago completo antes de finalizar.",
                    total.subtract(abonado)));
        }

        // Validar que la hora de la reserva ya haya llegado
        if (pedido.getFechaReserva() != null || pedido.getHoraReserva() != null) {
            java.time.ZoneId bogota = java.time.ZoneId.of("America/Bogota");
            java.time.LocalDate hoy = java.time.LocalDate.now(bogota);
            java.time.LocalDate fechaRes = java.time.LocalDate.parse(
                    pedido.getFechaReserva() != null ? pedido.getFechaReserva() : hoy.toString());
            boolean reservaFutura;
            if (fechaRes.isAfter(hoy)) {
                reservaFutura = true;
            } else if (fechaRes.isBefore(hoy)) {
                reservaFutura = false;
            } else { // misma fecha: revisar hora
                reservaFutura = pedido.getHoraReserva() != null &&
                        java.time.LocalTime.parse(pedido.getHoraReserva())
                                .isAfter(java.time.LocalTime.now(bogota));
            }
            if (reservaFutura) {
                String cuando = (pedido.getFechaReserva() != null && pedido.getHoraReserva() != null)
                        ? pedido.getFechaReserva() + " a las " + pedido.getHoraReserva()
                        : pedido.getFechaReserva() != null ? pedido.getFechaReserva()
                                : "las " + pedido.getHoraReserva();
                throw new ValidacionException(
                        "No se puede finalizar antes de la reserva (" + cuando + ")");
            }
        }

        descontarInventario(pedido);
        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setFinalizadoEn(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        Pedido guardado = pedidoRepo.save(pedido);

        // Un único cobro al fondo por moneda usada (solo el valor del pedido, no el
        // cambio)
        registrarCobroFinalPedido(guardado, total);

        auditoriaService.registrar("pedido", guardado.getId(), "FINALIZAR",
                "Pedido finalizado con total COP " + total);
        return pedidoMapper.toDTO(guardado);
    }

    @Transactional
    public PedidoDTO anularPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
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
        return pedidoMapper.toDTO(guardado);
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
        return pedidoMapper.toDTO(guardado);
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
        BigDecimal nuevaTasa = "USD".equals(nuevaMoneda) ? getTasaVigente() : null;
        BigDecimal nuevoMontoCop = monetaryService.convertirACop(nuevaMoneda, nuevoMonto, nuevaTasa);

        pago.setMoneda(nuevaMoneda);
        pago.setMonto(nuevoMonto);
        pago.setMontoCop(nuevoMontoCop);
        pago.setTasaUsada(nuevaTasa);

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_PAGO",
                "Pago actualizado: " + pagoId + " a " + nuevaMoneda + " " + nuevoMonto);
        return pedidoMapper.toDTO(guardado);
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
            return null;
        }
    }

    private void descontarInventario(Pedido pedido) {
        Map<String, BigDecimal> consumos = calcularConsumosInventario(pedido);
        validarStockDisponible(consumos);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarMovimientoInventario(consumo.getKey(), TipoMovimientoInventario.SALIDA, consumo.getValue(),
                    pedido.getId());
        }
    }

    private Map<String, BigDecimal> calcularConsumosInventario(Pedido pedido) {
        Map<String, BigDecimal> consumos = new LinkedHashMap<>();
        for (PedidoDetalle detalle : pedido.getDetalles()) {
            if (detalle.getComposicionSnapshot() == null || detalle.getComposicionSnapshot().isBlank())
                continue;
            for (Map<String, Object> item : leerComposicionSnapshot(detalle)) {
                Object insumoRaw = item.get("insumo");
                Object cantidadRaw = item.get("cantidad");
                if (!(insumoRaw instanceof String insumo) || insumo.isBlank()
                        || !(cantidadRaw instanceof Number)) {
                    throw new ValidacionException(
                            "Composicion de inventario invalida en el item: " + detalle.getNombreSnapshot());
                }
                BigDecimal cantPorUnidad = new BigDecimal(cantidadRaw.toString()).setScale(4, RoundingMode.HALF_UP);
                if (cantPorUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidacionException("Cantidad de inventario invalida para " + insumo + " en el item: "
                            + detalle.getNombreSnapshot());
                }
                consumos.merge(insumo.trim(), cantPorUnidad.multiply(BigDecimal.valueOf(detalle.getCantidad())),
                        BigDecimal::add);
            }
        }
        return consumos;
    }

    private List<Map<String, Object>> leerComposicionSnapshot(PedidoDetalle detalle) {
        try {
            return objectMapper.readValue(detalle.getComposicionSnapshot(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ValidacionException(
                    "No se pudo leer la composicion de inventario del item: " + detalle.getNombreSnapshot());
        }
    }

    private void validarStockDisponible(Map<String, BigDecimal> consumos) {
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            Inventario inv = inventarioRepo.findByInsumo(consumo.getKey())
                    .orElseThrow(() -> new ValidacionException(
                            "No existe inventario configurado para el insumo: " + consumo.getKey()));
            if (inv.getStockActual().compareTo(consumo.getValue()) < 0) {
                throw new ValidacionException("Stock insuficiente de " + consumo.getKey()
                        + ". Disponible: " + inv.getStockActual()
                        + ", requerido: " + consumo.getValue());
            }
        }
    }

    private void registrarMovimientoInventario(String insumo, TipoMovimientoInventario tipo, BigDecimal cantidad,
            Long pedidoId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(
                        () -> new ValidacionException("No existe inventario configurado para el insumo: " + insumo));
        if (tipo == TipoMovimientoInventario.SALIDA && inv.getStockActual().compareTo(cantidad) < 0) {
            throw new ValidacionException("Stock insuficiente de " + insumo
                    + ". Disponible: " + inv.getStockActual()
                    + ", requerido: " + cantidad);
        }
        BigDecimal nuevo = tipo == TipoMovimientoInventario.ENTRADA
                ? inv.getStockActual().add(cantidad)
                : inv.getStockActual().subtract(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);

        String obs = tipo == TipoMovimientoInventario.ENTRADA
                ? "Anulación pedido #" + pedidoId
                : "Finalización pedido #" + pedidoId;

        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo).tipo(tipo.valor()).cantidad(cantidad)
                .stockResultante(nuevo).referenciaTipo("pedido")
                .referenciaId(pedidoId).observacion(obs)
                .build());
    }

    /**
     * Al finalizar un pedido, registra UN único movimiento al fondo por cada moneda
     * usada.
     * Solo ingresa el valor del pedido (totalCop), distribuyendo proporcionalmente
     * entre
     * las monedas si hay pagos mixtos. El cambio nunca toca el fondo.
     */
    private void registrarCobroFinalPedido(Pedido pedido, BigDecimal totalCop) {
        // Agrupar pagos por moneda: [0] = suma monto original, [1] = suma montoCop
        Map<String, BigDecimal[]> porMoneda = new LinkedHashMap<>();
        for (Pago p : pedido.getPagos()) {
            porMoneda.compute(p.getMoneda(), (k, arr) -> {
                if (arr == null)
                    arr = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
                arr[0] = arr[0].add(p.getMonto());
                arr[1] = arr[1].add(p.getMontoCop());
                return arr;
            });
        }

        BigDecimal restante = totalCop;
        for (Map.Entry<String, BigDecimal[]> e : porMoneda.entrySet()) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0)
                break;
            String moneda = e.getKey();
            BigDecimal montoOrig = e.getValue()[0]; // en la moneda original
            BigDecimal montoCopTot = e.getValue()[1]; // equivalente COP total recibido

            BigDecimal montoCopFondo = montoCopTot.min(restante);
            BigDecimal montoFondo;
            if ("USD".equals(moneda) && montoCopTot.compareTo(BigDecimal.ZERO) > 0) {
                // Proporción: montoFondo_usd = montoOrig * (montoCopFondo / montoCopTot)
                montoFondo = montoOrig.multiply(montoCopFondo)
                        .divide(montoCopTot, 2, RoundingMode.FLOOR);
            } else {
                montoFondo = montoCopFondo; // COP y NEQUI: monto == montoCop
            }

            if (montoFondo.compareTo(BigDecimal.ZERO) > 0) {
                cajaMovimientoService.registrar(
                        CajaMovimientoTipo.COBRO_PEDIDO,
                        moneda,
                        montoFondo,
                        montoCopFondo,
                        "pedido",
                        pedido.getId(),
                        "Cobro pedido #" + pedido.getId(),
                        null);
                restante = restante.subtract(montoCopFondo);
            }
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
            return "COP";
        return "NEQUI".equalsIgnoreCase(moneda.trim()) ? fondoService.getCuentaNequiActiva() : moneda.trim();
    }

    private BigDecimal getTasaVigente() {
        return tasaCambioService.getValorVigente();
    }

    private void recalcularEstadoPago(Pedido pedido) {
        if (!pedido.getEstado().permiteEdicion())
            return;
        pedido.setEstado(
                EstadoPedido.calcular(pedidoMapper.calcularTotal(pedido), pedidoMapper.calcularAbonado(pedido)));
    }

    private void validarAbierto(Pedido pedido, String accion) {
        if (!pedido.getEstado().permiteEdicion())
            throw new ValidacionException("No se puede " + accion + " en un pedido " + pedido.getEstado());
    }

    private Pedido findOrThrow(Long id) {
        return pedidoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

}
