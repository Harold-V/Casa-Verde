package com.casaverde.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.CajaMovimiento;
import com.casaverde.backend.domain.CajaMovimientoTipo;
import com.casaverde.backend.domain.Fondo;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.CajaMovimientoRepository;
import com.casaverde.backend.repository.FondoRepository;
import com.casaverde.backend.repository.TasaCambioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CajaMovimientoService {

    private final FondoRepository fondoRepo;
    private final CajaMovimientoRepository cajaMovimientoRepo;
    private final TasaCambioRepository tasaRepo;
    private final MonetaryService monetaryService;

    @Transactional(propagation = Propagation.MANDATORY)
    public CajaMovimiento registrar(
            CajaMovimientoTipo tipo,
            String moneda,
            BigDecimal monto,
            BigDecimal montoCop,
            String referenciaTipo,
            Long referenciaId,
            String concepto,
            String observacion) {

        String monedaNormalizada = monetaryService.normalizarMoneda(moneda);
        BigDecimal montoNormalizado = monetaryService.normalizarSaldoFondo(monedaNormalizada, monto);
        if (montoNormalizado.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidacionException("El movimiento de caja no puede ser cero");
        }

        BigDecimal montoCopNormalizado = montoCop != null
                ? montoCop.setScale(0, java.math.RoundingMode.HALF_UP)
                : monetaryService.convertirACop(monedaNormalizada, montoNormalizado, getTasa());

        Fondo fondo = fondoRepo.findById(monedaNormalizada)
                .orElseGet(() -> Fondo.builder().moneda(monedaNormalizada).saldo(BigDecimal.ZERO).build());
        BigDecimal saldoResultante = fondo.getSaldo().add(montoNormalizado);
        fondo.setSaldo(saldoResultante);
        fondoRepo.save(fondo);

        return cajaMovimientoRepo.save(CajaMovimiento.builder()
                .tipo(tipo.name())
                .moneda(monedaNormalizada)
                .monto(montoNormalizado)
                .montoCop(montoCopNormalizado)
                .saldoResultante(saldoResultante)
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .concepto(concepto)
                .observacion(observacion)
                .build());
    }

    private BigDecimal getTasa() {
        return tasaRepo.findVigente()
                .orElseThrow(() -> new ValidacionException("No hay tasa de cambio configurada"))
                .getValorCop();
    }
}
