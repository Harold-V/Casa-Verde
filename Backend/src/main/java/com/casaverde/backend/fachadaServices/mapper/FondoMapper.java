package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.fachadaServices.dto.FondoDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoFondoDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface FondoMapper {

    FondoDTO toDTO(Fondo f);

    @Mapping(target = "tipo", expression = "java(resolveTipo(c.getTipo()))")
    @Mapping(target = "monto", expression = "java(c.getMonto().abs())")
    @Mapping(target = "montoCop", expression = "java(c.getMontoCop().abs())")
    @Mapping(target = "concepto", expression = "java(resolveConcepto(c))")
    @Mapping(target = "fecha", source = "registradoEn")
    MovimientoFondoDTO toMovimientoDTO(CajaMovimiento c);

    @Named("resolveTipo")
    default String resolveTipo(String tipoStr) {
        return switch (CajaMovimientoTipo.valueOf(tipoStr)) {
            case COBRO_PEDIDO -> "cobro";
            case GASTO -> "salida";
            case REVERSO_PAGO, REVERSO_GASTO -> "reverso";
            case AJUSTE_FONDO -> "ajuste";
            case ENTRADA_MANUAL -> "entrada";
            case TRANSFERENCIA_SALIDA -> "salida";
            case TRANSFERENCIA_ENTRADA -> "entrada";
        };
    }

    default String resolveConcepto(CajaMovimiento c) {
        return c.getObservacion() != null && !c.getObservacion().isBlank()
                ? c.getConcepto() + " - " + c.getObservacion()
                : c.getConcepto();
    }
}
