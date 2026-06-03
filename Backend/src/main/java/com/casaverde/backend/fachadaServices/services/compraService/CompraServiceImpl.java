package com.casaverde.backend.fachadaServices.services.compraService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.capaAccesoADatos.repositories.AbonoCompraRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.CompraRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;
import com.casaverde.backend.fachadaServices.mapper.AbonoCompraMapper;
import com.casaverde.backend.fachadaServices.mapper.CompraMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.pagoService.ICajaMovimientoService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements ICompraService {

    private final CompraRepository compraRepo;
    private final ProveedorRepository proveedorRepo;
    private final AbonoCompraRepository abonoRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IAuditoriaService auditoriaService;
    private final CompraMapper compraMapper;
    private final AbonoCompraMapper abonoMapper;

    @Transactional(readOnly = true)
    public List<CompraDTO> listar(LocalDate fecha, EstadoCompra estado) {
        List<Compra> compras;
        if (fecha != null && estado != null) {
            compras = compraRepo.findByFechaAndEstadoOrderByCreadoEnDesc(fecha, estado);
        } else if (fecha != null) {
            compras = compraRepo.findByFechaOrderByCreadoEnDesc(fecha);
        } else if (estado != null) {
            compras = compraRepo.findByEstadoOrderByCreadoEnDesc(estado);
        } else {
            compras = compraRepo.findAllByOrderByCreadoEnDesc();
        }
        return compras.stream().map(this::enrichCompraDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompraDTO obtener(Long id) {
        Compra compra = compraRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        return enrichCompraDTO(compra);
    }

    @Transactional
    public CompraDTO registrar(CompraRequest req) {
        var proveedor = proveedorRepo.findById(req.getProveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + req.getProveedorId()));
        if (!proveedor.getActivo())
            throw new ValidacionException("El proveedor está inactivo");

        String insumoNorm = req.getInsumo().trim().toLowerCase();
        if (!List.of("gallina", "cuy").contains(insumoNorm))
            throw new ValidacionException("El insumo debe ser 'gallina' o 'cuy'");

        BigDecimal totalCop = req.getCantidad().multiply(req.getPrecioUnitario())
                .setScale(0, RoundingMode.HALF_UP);

        Compra compra = compraRepo.save(Compra.builder()
                .proveedor(proveedor)
                .proveedorNombre(proveedor.getNombre())
                .insumo(insumoNorm)
                .cantidad(req.getCantidad())
                .precioUnitario(req.getPrecioUnitario().setScale(0, RoundingMode.HALF_UP))
                .totalCop(totalCop)
                .estado(EstadoCompra.PENDIENTE)
                .observacion(req.getObservacion())
                .fecha(req.getFecha())
                .build());

        sumarInventario(insumoNorm, req.getCantidad(), compra.getId());
        auditoriaService.registrar("Compra", compra.getId(),
                "REGISTRAR", "Compra de " + req.getCantidad() + " " + insumoNorm
                        + " a " + proveedor.getNombre() + " por $" + totalCop);

        return enrichCompraDTO(compra);
    }

    @Transactional
    public void anular(Long id) {
        Compra compra = compraRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        if (compra.getEstado() != EstadoCompra.PENDIENTE)
            throw new ValidacionException("Solo se pueden anular compras sin abonos (estado PENDIENTE)");
        if (!abonoRepo.findByCompraIdOrderByCreadoEnDesc(id).isEmpty())
            throw new ValidacionException("No se puede anular una compra que tiene abonos registrados");

        revertirInventario(compra.getInsumo(), compra.getCantidad(), compra.getId());
        auditoriaService.registrar("Compra", compra.getId(), "ANULAR",
                "Compra anulada: " + compra.getCantidad() + " " + compra.getInsumo());
        compraRepo.delete(compra);
    }

    @Transactional
    public AbonoCompraDTO registrarAbono(Long compraId, AbonoCompraRequest req) {
        Compra compra = compraRepo.findById(compraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + compraId));
        if (compra.getEstado() == EstadoCompra.PAGADO)
            throw new ValidacionException("La compra ya está pagada completamente");

        String moneda = req.getMoneda().trim().toUpperCase();

        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoCop = "USD".equals(moneda)
                ? req.getMonto().multiply(tasaUsd).setScale(0, RoundingMode.HALF_UP)
                : req.getMonto().setScale(0, RoundingMode.HALF_UP);
        BigDecimal tasaUsada = "USD".equals(moneda) ? tasaUsd : null;

        BigDecimal totalAbonado = abonoRepo.sumMontoCopByCompraId(compraId);
        BigDecimal saldoPendiente = compra.getTotalCop().subtract(totalAbonado);
        if (montoCop.compareTo(saldoPendiente) > 0)
            throw new ValidacionException(
                    "El abono ($" + montoCop + ") supera el saldo pendiente ($" + saldoPendiente + ")");

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.COMPRA_PAGO,
                moneda,
                req.getMonto().negate(),
                montoCop.negate(),
                "compra",
                compraId,
                "Pago compra #" + compraId + " (" + compra.getInsumo() + ")",
                null);

        AbonoCompra abono = abonoRepo.save(AbonoCompra.builder()
                .compra(compra)
                .monto(req.getMonto())
                .moneda(moneda)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .fondoDebitado(moneda)
                .fecha(req.getFecha())
                .build());

        BigDecimal nuevoTotal = totalAbonado.add(montoCop);
        EstadoCompra nuevoEstado = nuevoTotal.compareTo(compra.getTotalCop()) >= 0
                ? EstadoCompra.PAGADO
                : EstadoCompra.ABONADO_PARCIAL;
        compra.setEstado(nuevoEstado);
        compraRepo.save(compra);

        auditoriaService.registrar("AbonoCompra", abono.getId(), "REGISTRAR",
                "Abono $" + req.getMonto() + " " + moneda + " a compra #" + compraId);
        return abonoMapper.toDTO(abono);
    }

    @Transactional(readOnly = true)
    public List<AbonoCompraDTO> listarAbonos(Long compraId) {
        if (!compraRepo.existsById(compraId))
            throw new RecursoNoEncontradoException("Compra no encontrada: " + compraId);
        return abonoRepo.findByCompraIdOrderByCreadoEnDesc(compraId).stream()
                .map(abonoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // --- helpers ---

    private CompraDTO enrichCompraDTO(Compra compra) {
        CompraDTO dto = compraMapper.toDTO(compra);
        BigDecimal totalAbonado = abonoRepo.sumMontoCopByCompraId(compra.getId());
        dto.setTotalAbonado(totalAbonado);
        dto.setSaldoPendiente(compra.getTotalCop().subtract(totalAbonado));
        List<AbonoCompraDTO> abonos = abonoRepo.findByCompraIdOrderByCreadoEnDesc(compra.getId())
                .stream().map(abonoMapper::toDTO).collect(Collectors.toList());
        dto.setAbonos(abonos);
        return dto;
    }

    private void sumarInventario(String insumo, BigDecimal cantidad, Long compraId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("Insumo no encontrado en inventario: " + insumo));
        BigDecimal nuevo = inv.getStockActual().add(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo("entrada")
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("compra")
                .referenciaId(compraId)
                .observacion("Compra #" + compraId)
                .build());
    }

    private void revertirInventario(String insumo, BigDecimal cantidad, Long compraId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("Insumo no encontrado en inventario: " + insumo));
        BigDecimal nuevo = inv.getStockActual().subtract(cantidad);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0)
            nuevo = BigDecimal.ZERO;
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo("ajuste")
                .cantidad(nuevo)
                .stockResultante(nuevo)
                .referenciaTipo("compra_anulada")
                .referenciaId(compraId)
                .observacion("Anulación compra #" + compraId)
                .build());
    }
}
