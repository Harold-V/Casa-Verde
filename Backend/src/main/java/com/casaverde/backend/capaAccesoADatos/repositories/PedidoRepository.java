package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByEstadoOrderByCreadoEnDesc(EstadoPedido estado);

    List<Pedido> findByEstadoInOrderByCreadoEnDesc(List<EstadoPedido> estados);

    List<Pedido> findAllByOrderByCreadoEnDesc();

    List<Pedido> findByCreadoEnBetweenOrderByCreadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Pedido> findByFinalizadoEnBetweenOrderByFinalizadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    long countByEstadoIn(List<EstadoPedido> estados);
}
