package com.casaverde.backend.fachadaServices.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.casaverde.backend.capaAccesoADatos.models.entitys.*;
import com.casaverde.backend.fachadaServices.dto.*;

@Mapper(componentModel = "spring")
public interface EmpleadoMapper {

    EmpleadoDTO toDTO(Empleado e);

    @Mapping(source = "empleado.id", target = "empleadoId")
    AdelantoDTO toAdelantoDTO(Adelanto a);

    @Mapping(source = "empleado.id", target = "empleadoId")
    TrabajoDiaDTO toTrabajoDiaDTO(TrabajoDia t);

    @Mapping(source = "empleado.id", target = "empleadoId")
    PagoEmpleadoDTO toPagoDTO(PagoEmpleado p);

    @Mapping(source = "empleado.id", target = "empleadoId")
    VacacionDTO toVacacionDTO(Vacacion v);
}
