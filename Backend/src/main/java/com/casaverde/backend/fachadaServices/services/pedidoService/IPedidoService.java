package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.ActualizarDetalleRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;

public interface IPedidoService {

    List<PedidoDTO> listarActivos();

    List<PedidoDTO> listarTodos();

    PedidoDTO obtener(Long id);

    String comprobanteTexto(Long id);

    PedidoDTO crear(CrearPedidoRequest req);

    PedidoDTO actualizarPedido(Long pedidoId, ActualizarPedidoRequest req);

    PedidoDTO agregarDetalle(Long pedidoId, DetalleItemRequest req);

    PedidoDTO quitarDetalle(Long pedidoId, Long detalleId);

    PedidoDTO actualizarDetalle(Long pedidoId, Long detalleId, ActualizarDetalleRequest req);

    PedidoDTO registrarPago(Long pedidoId, RegistrarPagoRequest req);

    PedidoDTO finalizarPedido(Long pedidoId);

    PedidoDTO anularPedido(Long pedidoId);

    PedidoDTO quitarPago(Long pedidoId, Long pagoId);

    PedidoDTO actualizarPago(Long pedidoId, Long pagoId, ActualizarPagoRequest req);
}
