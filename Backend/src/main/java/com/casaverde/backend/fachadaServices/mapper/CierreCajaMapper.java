package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CierreCaja;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CierreCajaMapper {
    CierreCajaDTO toDTO(CierreCaja c);
}
