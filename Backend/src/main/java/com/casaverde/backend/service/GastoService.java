package com.casaverde.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.CajaMovimientoTipo;
import com.casaverde.backend.domain.Gasto;
import com.casaverde.backend.domain.Inventario;
import com.casaverde.backend.domain.MovimientoInventario;
import com.casaverde.backend.dto.GastoDTO;
import com.casaverde.backend.dto.GastoRequest;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.GastoRepository;
import com.casaverde.backend.repository.InventarioRepository;
import com.casaverde.backend.repository.MovimientoInventarioRepository;
import com.casaverde.backend.repository.TasaCambioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GastoService {

    private static final List<String> CATEGORIAS = List.of("insumos", "servicios", "equipos", "otros");
    private static final List<String> MONEDAS    = List.of("COP", "USD", "NEQUI");

    private final GastoRepository gastoRepo;
    private final TasaCambioRepository tasaRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoInventarioRepo;
    private final CajaMovimientoService cajaMovimientoService;
    private final MonetaryService monetaryService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<GastoDTO> listarTodos() {
        return gastoRepo.findAllByOrderByRegistradoEnDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GastoDTO> listarPorFecha(LocalDate fecha) {
        LocalDateTime ini = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
        return gastoRepo.findByRegistradoEnBetweenOrderByRegistradoEnDesc(ini, fin)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public GastoDTO registrar(GastoRequest req) {
        if (req.getMoneda() != null) {
            req.setMoneda(monetaryService.normalizarMoneda(req.getMoneda()));
        }
        validar(req);
        validarInventario(req);

        String moneda = monetaryService.normalizarMoneda(req.getMoneda());
        BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getMonto());
        BigDecimal montoCop = calcularMontoCop(moneda, monto);
        BigDecimal tasaUsada = "USD".equals(moneda) ? getTasa() : null;

        Gasto gasto = Gasto.builder()
                .concepto(req.getConcepto().trim())
                .categoria(req.getCategoria())
                .moneda(moneda)
                .monto(monto)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .insumoInventario(normalizarInsumo(req.getInsumoInventario()))
                .cantidadInventario(req.getCantidadInventario())
                .observacion(req.getObservacion())
                .build();

        Gasto guardado = gastoRepo.save(gasto);
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.GASTO,
                moneda,
                monto.negate(),
                montoCop.negate(),
                "gasto",
                guardado.getId(),
                "Gasto: " + guardado.getConcepto(),
                guardado.getObservacion());
        registrarEntradaInventarioSiAplica(guardado);
        auditoriaService.registrar("gasto", guardado.getId(), "REGISTRAR",
                "Gasto " + moneda + " " + monto + " concepto " + guardado.getConcepto());
        return toDTO(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        Gasto gasto = gastoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto no encontrado: " + id));
        revertirInventarioSiAplica(gasto);
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.REVERSO_GASTO,
                gasto.getMoneda(),
                gasto.getMonto(),
                gasto.getMontoCop(),
                "gasto",
                gasto.getId(),
                "Reverso gasto eliminado: " + gasto.getConcepto(),
                null);
        gastoRepo.delete(gasto);
        auditoriaService.registrar("gasto", id, "ELIMINAR",
                "Gasto eliminado y fondo revertido: " + gasto.getConcepto());
    }

    // ---- helpers ----

    private void validar(GastoRequest req) {
        if (req.getConcepto() == null || req.getConcepto().isBlank())
            throw new ValidacionException("El concepto es obligatorio");
        if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto debe ser mayor a 0");
        if (!CATEGORIAS.contains(req.getCategoria()))
            throw new ValidacionException("Categoría inválida. Use: " + CATEGORIAS);
        if (!MONEDAS.contains(req.getMoneda()))
            throw new ValidacionException("Moneda inválida. Use: COP, USD, NEQUI");
    }

    private void validarInventario(GastoRequest req) {
        boolean tieneInsumo = req.getInsumoInventario() != null && !req.getInsumoInventario().isBlank();
        boolean tieneCantidad = req.getCantidadInventario() != null;
        if (!tieneInsumo && !tieneCantidad) return;
        if (!tieneInsumo || !tieneCantidad) {
            throw new ValidacionException("Para afectar inventario informe insumoInventario y cantidadInventario");
        }
        if (req.getCantidadInventario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("La cantidad de inventario debe ser mayor a 0");
        }
    }

    private BigDecimal calcularMontoCop(String moneda, BigDecimal monto) {
        return monetaryService.convertirACop(moneda, monto, getTasa());
    }

    private BigDecimal getTasa() {
        return tasaRepo.findVigente()
                .orElseThrow(() -> new ValidacionException("No hay tasa de cambio configurada"))
                .getValorCop();
    }

    private void registrarEntradaInventarioSiAplica(Gasto gasto) {
        if (!afectaInventario(gasto)) return;
        Inventario inv = inventarioRepo.findByInsumo(gasto.getInsumoInventario())
                .orElseGet(() -> inventarioRepo.save(Inventario.builder()
                        .insumo(gasto.getInsumoInventario())
                        .stockActual(BigDecimal.ZERO)
                        .unidad("unidad")
                        .build()));
        BigDecimal nuevoStock = inv.getStockActual().add(gasto.getCantidadInventario());
        inv.setStockActual(nuevoStock);
        inventarioRepo.save(inv);

        movimientoInventarioRepo.save(MovimientoInventario.builder()
                .insumo(gasto.getInsumoInventario())
                .tipo("entrada")
                .cantidad(gasto.getCantidadInventario())
                .stockResultante(nuevoStock)
                .referenciaTipo("gasto")
                .referenciaId(gasto.getId())
                .observacion("Compra inventariable: " + gasto.getConcepto())
                .build());
    }

    private void revertirInventarioSiAplica(Gasto gasto) {
        if (!afectaInventario(gasto)) return;
        Inventario inv = inventarioRepo.findByInsumo(gasto.getInsumoInventario())
                .orElseThrow(() -> new ValidacionException("No existe inventario para revertir el gasto: " + gasto.getInsumoInventario()));
        if (inv.getStockActual().compareTo(gasto.getCantidadInventario()) < 0) {
            throw new ValidacionException("No se puede eliminar el gasto porque el stock de "
                    + gasto.getInsumoInventario()
                    + " ya fue consumido. Stock actual: " + inv.getStockActual()
                    + ", reverso requerido: " + gasto.getCantidadInventario());
        }
        BigDecimal nuevoStock = inv.getStockActual().subtract(gasto.getCantidadInventario());
        inv.setStockActual(nuevoStock);
        inventarioRepo.save(inv);

        movimientoInventarioRepo.save(MovimientoInventario.builder()
                .insumo(gasto.getInsumoInventario())
                .tipo("salida")
                .cantidad(gasto.getCantidadInventario())
                .stockResultante(nuevoStock)
                .referenciaTipo("gasto")
                .referenciaId(gasto.getId())
                .observacion("Reverso de gasto eliminado: " + gasto.getConcepto())
                .build());
    }

    private boolean afectaInventario(Gasto gasto) {
        return gasto.getInsumoInventario() != null
                && !gasto.getInsumoInventario().isBlank()
                && gasto.getCantidadInventario() != null
                && gasto.getCantidadInventario().compareTo(BigDecimal.ZERO) > 0;
    }

    private String normalizarInsumo(String insumo) {
        return insumo == null || insumo.isBlank() ? null : insumo.trim().toLowerCase();
    }

    private GastoDTO toDTO(Gasto g) {
        return GastoDTO.builder()
                .id(g.getId())
                .concepto(g.getConcepto())
                .categoria(g.getCategoria())
                .moneda(g.getMoneda())
                .monto(g.getMonto())
                .montoCop(g.getMontoCop())
                .tasaUsada(g.getTasaUsada())
                .insumoInventario(g.getInsumoInventario())
                .cantidadInventario(g.getCantidadInventario())
                .observacion(g.getObservacion())
                .registradoEn(g.getRegistradoEn())
                .build();
    }
}
