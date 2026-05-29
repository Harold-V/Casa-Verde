package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;

public interface IAuditoriaService {

    List<HistorialAuditoriaDTO> listar(String entidad, Long entidadId);

    void registrar(String entidad, Long entidadId, String accion, String detalle);
}
