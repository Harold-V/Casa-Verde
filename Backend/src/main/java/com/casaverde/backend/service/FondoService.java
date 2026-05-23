package com.casaverde.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.CajaMovimiento;
import com.casaverde.backend.domain.CajaMovimientoTipo;
import com.casaverde.backend.domain.Fondo;
import com.casaverde.backend.domain.Gasto;
import com.casaverde.backend.domain.Pago;
import com.casaverde.backend.dto.AjusteCapitalRequest;
import com.casaverde.backend.dto.FondoDTO;
import com.casaverde.backend.dto.MovimientoFondoDTO;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.CajaMovimientoRepository;
import com.casaverde.backend.repository.FondoRepository;
import com.casaverde.backend.repository.GastoRepository;
import com.casaverde.backend.repository.PagoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FondoService {

    private final FondoRepository fondoRepo;
    private final PagoRepository pagoRepo;
    private final GastoRepository gastoRepo;
    private final CajaMovimientoRepository cajaMovimientoRepo;
    private final CajaMovimientoService cajaMovimientoService;
    private final MonetaryService monetaryService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<FondoDTO> listar() {
        return fondoRepo.findAll().stream()
                .map(this::toDTO)
                .sorted(Comparator.comparing(FondoDTO::getMoneda))
                .collect(Collectors.toList());
    }

    @Transactional
    public FondoDTO ajustar(String moneda, AjusteCapitalRequest req) {
        String monedaNormalizada = monetaryService.normalizarMoneda(moneda);
        if (req.getNuevoSaldo() == null)
            throw new ValidacionException("El nuevo saldo es obligatorio");
        BigDecimal nuevoSaldo = monetaryService.normalizarSaldoFondo(monedaNormalizada, req.getNuevoSaldo());
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidacionException("El saldo del fondo no puede ser negativo");

        Fondo fondo = fondoRepo.findById(monedaNormalizada)
                .orElseThrow(() -> new RecursoNoEncontradoException("Fondo no encontrado: " + monedaNormalizada));

        BigDecimal saldoAnterior = fondo.getSaldo();
        BigDecimal delta = nuevoSaldo.subtract(saldoAnterior);

        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            cajaMovimientoService.registrar(
                    CajaMovimientoTipo.AJUSTE_FONDO,
                    monedaNormalizada,
                    delta,
                    null,
                    "fondo",
                    null,
                    "Ajuste manual fondo " + monedaNormalizada,
                    req.getMotivo());
        }

        auditoriaService.registrar("fondo", null, "AJUSTAR",
                "Fondo " + monedaNormalizada + " ajustado de " + saldoAnterior + " a " + nuevoSaldo);
        return toDTO(fondoRepo.findById(monedaNormalizada).orElse(fondo));
    }

    @Transactional(readOnly = true)
    public List<MovimientoFondoDTO> historial() {
        List<Pago>  pagos  = pagoRepo.findAllConPedidoOrderByRegistradoEnDesc();
        List<Gasto> gastos = gastoRepo.findAllByOrderByRegistradoEnDesc();
        List<CajaMovimiento> caja = cajaMovimientoRepo.findAllByOrderByRegistradoEnDesc();

        List<MovimientoFondoDTO> movimientos = new ArrayList<>();

        for (Pago p : pagos) {
            if (cajaMovimientoRepo.existsByReferenciaTipoAndReferenciaIdAndTipo(
                    "pago", p.getId(), CajaMovimientoTipo.COBRO_PEDIDO.name())) {
                continue;
            }
            String concepto = "Cobro pedido #" + (p.getPedido() != null ? p.getPedido().getId() : "?");
            movimientos.add(MovimientoFondoDTO.builder()
                    .tipo("cobro")
                    .moneda(p.getMoneda())
                    .monto(p.getMonto())
                    .montoCop(p.getMontoCop())
                    .concepto(concepto)
                    .fecha(p.getRegistradoEn())
                    .build());
        }

        for (Gasto g : gastos) {
            if (cajaMovimientoRepo.existsByReferenciaTipoAndReferenciaIdAndTipo(
                    "gasto", g.getId(), CajaMovimientoTipo.GASTO.name())) {
                continue;
            }
            movimientos.add(MovimientoFondoDTO.builder()
                    .tipo("gasto")
                    .moneda(g.getMoneda())
                    .monto(g.getMonto())
                    .montoCop(g.getMontoCop())
                    .concepto(g.getConcepto())
                    .fecha(g.getRegistradoEn())
                    .build());
        }

        for (CajaMovimiento c : caja) {
            boolean entrada = c.getMonto().compareTo(BigDecimal.ZERO) >= 0;
            String concepto = c.getConcepto();
            if (c.getObservacion() != null && !c.getObservacion().isBlank()) {
                concepto += " - " + c.getObservacion();
            }
            movimientos.add(MovimientoFondoDTO.builder()
                    .tipo(entrada ? "cobro" : "gasto")
                    .moneda(c.getMoneda())
                    .monto(c.getMonto().abs())
                    .montoCop(c.getMontoCop().abs())
                    .concepto(concepto)
                    .fecha(c.getRegistradoEn())
                    .build());
        }

        movimientos.sort(Comparator.comparing(MovimientoFondoDTO::getFecha).reversed());
        return movimientos;
    }

    private FondoDTO toDTO(Fondo f) {
        return FondoDTO.builder()
                .moneda(f.getMoneda())
                .saldo(f.getSaldo())
                .actualizadoEn(f.getActualizadoEn())
                .build();
    }
}
