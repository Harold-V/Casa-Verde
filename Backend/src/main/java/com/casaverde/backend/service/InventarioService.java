package com.casaverde.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.Inventario;
import com.casaverde.backend.domain.MovimientoInventario;
import com.casaverde.backend.dto.InventarioDTO;
import com.casaverde.backend.dto.MovimientoInventarioDTO;
import com.casaverde.backend.dto.MovimientoRequest;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.InventarioRepository;
import com.casaverde.backend.repository.MovimientoInventarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;

    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<InventarioDTO> listarStock() {
        return inventarioRepo.findAllByOrderByInsumoAsc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventarioDTO obtenerStock(String insumo) {
        return toDTO(findInsumoOrThrow(insumo));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarMovimientos() {
        return movimientoRepo.findAllByOrderByRegistradoEnDesc().stream()
                .map(this::toMovDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarMovimientosPorInsumo(String insumo) {
        return movimientoRepo.findByInsumoOrderByRegistradoEnDesc(insumo).stream()
                .map(this::toMovDTO).collect(Collectors.toList());
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
        return toMovDTO(guardado);
    }

    private BigDecimal calcularNuevoStock(BigDecimal actual, MovimientoRequest req) {
        return switch (req.getTipo()) {
            case "entrada" -> actual.add(req.getCantidad());
            case "salida"  -> actual.subtract(req.getCantidad());
            case "ajuste"  -> req.getCantidad();
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

    private InventarioDTO toDTO(Inventario i) {
        return InventarioDTO.builder()
                .id(i.getId()).insumo(i.getInsumo())
                .stockActual(i.getStockActual()).unidad(i.getUnidad())
                .actualizadoEn(i.getActualizadoEn())
                .build();
    }

    private MovimientoInventarioDTO toMovDTO(MovimientoInventario m) {
        return MovimientoInventarioDTO.builder()
                .id(m.getId()).insumo(m.getInsumo()).tipo(m.getTipo())
                .cantidad(m.getCantidad()).stockResultante(m.getStockResultante())
                .referenciaTipo(m.getReferenciaTipo()).referenciaId(m.getReferenciaId())
                .observacion(m.getObservacion()).registradoEn(m.getRegistradoEn())
                .build();
    }
}
