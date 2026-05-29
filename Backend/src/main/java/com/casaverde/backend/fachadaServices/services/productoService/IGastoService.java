package com.casaverde.backend.fachadaServices.services.productoService;

import java.time.LocalDate;
import java.util.List;

import com.casaverde.backend.fachadaServices.dto.GastoDTO;
import com.casaverde.backend.fachadaServices.dto.GastoRequest;

public interface IGastoService {

    List<GastoDTO> listarTodos();

    List<GastoDTO> listarPorFecha(LocalDate fecha);

    GastoDTO registrar(GastoRequest req);

    void eliminar(Long id);
}
