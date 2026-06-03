package com.casaverde.backend.fachadaServices.services.empleadoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.*;

public interface IEmpleadoService {

    List<EmpleadoDTO> listar(boolean soloActivos);
    EmpleadoDTO crear(EmpleadoRequest req);
    EmpleadoDTO actualizar(Long id, EmpleadoRequest req);
    EmpleadoDTO toggle(Long id);

    List<AdelantoDTO> listarAdelantos(Long empleadoId);
    AdelantoDTO registrarAdelanto(Long empleadoId, AdelantoRequest req);

    List<TrabajoDiaDTO> listarDias(Long empleadoId, boolean soloNoPagados);
    TrabajoDiaDTO registrarDia(Long empleadoId, TrabajoDiaRequest req);
    void eliminarDia(Long empleadoId, Long diaId);

    List<PagoEmpleadoDTO> listarPagos(Long empleadoId);
    EmpleadoResumenDTO previewPago(Long empleadoId);
    PagoEmpleadoDTO liquidar(Long empleadoId, PagoEmpleadoRequest req);

    List<VacacionDTO> listarVacaciones(Long empleadoId);
    VacacionDTO registrarVacacion(Long empleadoId, VacacionRequest req);
    void eliminarVacacion(Long empleadoId, Long vacId);
}
