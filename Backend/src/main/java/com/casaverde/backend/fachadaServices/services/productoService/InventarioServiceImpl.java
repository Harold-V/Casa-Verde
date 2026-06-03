package com.casaverde.backend.fachadaServices.services.productoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoMovimientoInventario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.InventarioMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements IInventarioService {

    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final IAuditoriaService auditoriaService;
    private final InventarioMapper inventarioMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<InventarioDTO> listarStock() {
        return inventarioRepo.findAllByOrderByInsumoAsc().stream()
                .map(inventarioMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventarioDTO obtenerStock(String insumo) {
        return inventarioMapper.toDTO(findInsumoOrThrow(insumo));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarMovimientos() {
        return movimientoRepo.findAllByOrderByRegistradoEnDesc().stream()
                .map(inventarioMapper::toMovimientoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarMovimientosPorInsumo(String insumo) {
        return movimientoRepo.findByInsumoOrderByRegistradoEnDesc(insumo).stream()
                .map(inventarioMapper::toMovimientoDTO).collect(Collectors.toList());
    }

    @Transactional
    public MovimientoInventarioDTO registrarMovimiento(MovimientoRequest req) {
        validarRequest(req);

        Inventario inv = inventarioRepo.findByInsumo(req.getInsumo())
                .orElseGet(() -> {
                    Inventario nuevo = Inventario.builder()
                            .insumo(req.getInsumo().trim())
                            .stockActual(BigDecimal.ZERO)
                            .unidad("unidad")
                            .build();
                    return inventarioRepo.save(nuevo);
                });

        BigDecimal nuevoStock = calcularNuevoStock(inv.getStockActual(), req);
        if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("Stock insuficiente. Stock actual: " + inv.getStockActual()
                    + " — solicitado: " + req.getCantidad());
        }

        inv.setStockActual(nuevoStock);
        inventarioRepo.save(inv);

        MovimientoInventario mov = MovimientoInventario.builder()
                .insumo(req.getInsumo().trim())
                .tipo(req.getTipo())
                .cantidad(req.getCantidad())
                .stockResultante(nuevoStock)
                .referenciaTipo("ajuste_manual")
                .observacion(req.getObservacion())
                .build();

        MovimientoInventario guardado = movimientoRepo.save(mov);
        auditoriaService.registrar("inventario", inv.getId(), "MOVIMIENTO_MANUAL",
                req.getTipo() + " " + req.getCantidad() + " " + req.getInsumo());
        return inventarioMapper.toMovimientoDTO(guardado);
    }

    private BigDecimal calcularNuevoStock(BigDecimal actual, MovimientoRequest req) {
        return switch (req.getTipo()) {
            case "entrada" -> actual.add(req.getCantidad());
            case "salida" -> actual.subtract(req.getCantidad());
            case "ajuste" -> req.getCantidad();
            default -> throw new ValidacionException("Tipo de movimiento inválido: " + req.getTipo());
        };
    }

    private void validarRequest(MovimientoRequest req) {
        if (req.getInsumo() == null || req.getInsumo().isBlank()) {
            throw new ValidacionException("El insumo es obligatorio");
        }
        if (req.getCantidad() == null || req.getCantidad().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("La cantidad debe ser mayor o igual a 0");
        }
        if (req.getTipo() == null || !List.of("entrada", "salida", "ajuste").contains(req.getTipo())) {
            throw new ValidacionException("Tipo inválido. Use: entrada, salida, ajuste");
        }
    }

    private Inventario findInsumoOrThrow(String insumo) {
        return inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + insumo));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void descontarPorPedido(Pedido pedido) {
        Map<String, BigDecimal> consumos = calcularConsumos(pedido);
        validarStock(consumos);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarSalida(consumo.getKey(), consumo.getValue(), pedido.getId());
        }
    }

    private Map<String, BigDecimal> calcularConsumos(Pedido pedido) {
        Map<String, BigDecimal> consumos = new LinkedHashMap<>();
        for (PedidoDetalle detalle : pedido.getDetalles()) {
            if (detalle.getComposicionSnapshot() == null || detalle.getComposicionSnapshot().isBlank())
                continue;
            for (Map<String, Object> item : leerSnapshot(detalle)) {
                Object insumoRaw = item.get("insumo");
                Object cantidadRaw = item.get("cantidad");
                if (!(insumoRaw instanceof String insumo) || insumo.isBlank()
                        || !(cantidadRaw instanceof Number)) {
                    throw new ValidacionException(
                            "Composicion de inventario invalida en el item: " + detalle.getNombreSnapshot());
                }
                BigDecimal cantPorUnidad = new BigDecimal(cantidadRaw.toString()).setScale(4, RoundingMode.HALF_UP);
                if (cantPorUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidacionException("Cantidad de inventario invalida para " + insumo
                            + " en el item: " + detalle.getNombreSnapshot());
                }
                consumos.merge(insumo.trim(),
                        cantPorUnidad.multiply(BigDecimal.valueOf(detalle.getCantidad())),
                        BigDecimal::add);
            }
        }
        return consumos;
    }

    private List<Map<String, Object>> leerSnapshot(PedidoDetalle detalle) {
        try {
            return objectMapper.readValue(detalle.getComposicionSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ValidacionException(
                    "No se pudo leer la composicion de inventario del item: " + detalle.getNombreSnapshot());
        }
    }

    private void validarStock(Map<String, BigDecimal> consumos) {
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

    @Transactional
    @Override
    public InventarioDTO actualizarAlerta(String insumo, BigDecimal alertaMinima) {
        if (alertaMinima == null || alertaMinima.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("La alerta mínima debe ser un valor positivo o cero");
        }
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + insumo));
        inv.setAlertaMinima(alertaMinima);
        return inventarioMapper.toDTO(inventarioRepo.save(inv));
    }

    private void registrarSalida(String insumo, BigDecimal cantidad, Long pedidoId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException(
                        "No existe inventario configurado para el insumo: " + insumo));
        if (inv.getStockActual().compareTo(cantidad) < 0) {
            throw new ValidacionException("Stock insuficiente de " + insumo
                    + ". Disponible: " + inv.getStockActual()
                    + ", requerido: " + cantidad);
        }
        BigDecimal nuevo = inv.getStockActual().subtract(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo(TipoMovimientoInventario.SALIDA.valor())
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("pedido")
                .referenciaId(pedidoId)
                .observacion("Finalización pedido #" + pedidoId)
                .build());
    }
}
