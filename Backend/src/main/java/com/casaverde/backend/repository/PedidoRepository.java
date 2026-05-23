package com.casaverde.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByEstadoOrderByCreadoEnDesc(String estado);
    List<Pedido> findByEstadoInOrderByCreadoEnDesc(List<String> estados);
    List<Pedido> findAllByOrderByCreadoEnDesc();
    List<Pedido> findByCreadoEnBetweenOrderByCreadoEnDesc(LocalDateTime inicio, LocalDateTime fin);
    List<Pedido> findByFinalizadoEnBetweenOrderByFinalizadoEnDesc(LocalDateTime inicio, LocalDateTime fin);
}
