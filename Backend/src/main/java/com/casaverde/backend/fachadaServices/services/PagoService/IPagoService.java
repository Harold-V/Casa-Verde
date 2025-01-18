package com.casaverde.backend.fachadaServices.services.PagoService;

import com.casaverde.backend.fachadaServices.DTO.PagoDTO;

import java.util.List;

public interface IPagoService {
    List<PagoDTO> findAll();

    PagoDTO findById(Long id);

    PagoDTO save(PagoDTO pago);

    PagoDTO update(Long id, PagoDTO pago);

    boolean delete(Long id);
}