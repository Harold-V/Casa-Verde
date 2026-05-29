package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;

public interface IInventarioService {

    List<InventarioDTO> listarStock();

    InventarioDTO obtenerStock(String insumo);

    List<MovimientoInventarioDTO> listarMovimientos();

    List<MovimientoInventarioDTO> listarMovimientosPorInsumo(String insumo);

    MovimientoInventarioDTO registrarMovimiento(MovimientoRequest req);
}
