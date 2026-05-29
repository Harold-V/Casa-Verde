package com.casaverde.backend.fachadaServices.services.productoService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
}
