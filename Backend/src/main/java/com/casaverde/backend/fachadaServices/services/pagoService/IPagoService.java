package com.casaverde.backend.fachadaServices.services.pagoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.PagoDTO;

public interface IPagoService {
    List<PagoDTO> findAll();

    PagoDTO findById(Long id);

    PagoDTO save(PagoDTO pago);

    PagoDTO update(Long id, PagoDTO pago);

    boolean delete(Long id);
}