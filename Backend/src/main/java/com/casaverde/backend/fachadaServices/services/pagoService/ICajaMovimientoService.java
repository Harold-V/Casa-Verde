package com.casaverde.backend.fachadaServices.services.pagoService;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;

public interface ICajaMovimientoService {

    CajaMovimiento registrar(
            CajaMovimientoTipo tipo,
            String moneda,
            BigDecimal monto,
            BigDecimal montoCop,
            String referenciaTipo,
            Long referenciaId,
            String concepto,
            String observacion);
}
