package com.casaverde.backend.fachadaServices.services.PedidoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.DTO.PedidoDTO;

public interface IPedidoService {
    public List<PedidoDTO> findAll();

    public PedidoDTO findById(Long id);

    public PedidoDTO save(PedidoDTO pedido);

    public PedidoDTO update(Long id, PedidoDTO pedido);

    public boolean updateEstado(Long id);
}
