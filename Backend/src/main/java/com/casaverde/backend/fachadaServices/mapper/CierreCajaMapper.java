package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CierreCaja;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CierreCajaMapper {

    @Mapping(target = "paraEntregar", ignore = true)
    CierreCajaDTO toDTO(CierreCaja c);

    @AfterMapping
    default void calcularParaEntregar(CierreCaja c, @MappingTarget CierreCajaDTO dto) {
        if (c.getBaseCop() != null && c.getContadoCop() != null) {
            dto.setParaEntregar(c.getContadoCop().subtract(c.getBaseCop()));
        }
    }
}
