package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = { "salon", "detalles", "pagos" })
    List<Pedido> findByEstadoOrderByCreadoEnDesc(EstadoPedido estado);

    @EntityGraph(attributePaths = { "salon", "detalles", "pagos" })
    List<Pedido> findByEstadoInOrderByCreadoEnDesc(List<EstadoPedido> estados);

    @EntityGraph(attributePaths = { "salon", "detalles", "pagos" })
    List<Pedido> findAllByOrderByCreadoEnDesc();

    @EntityGraph(attributePaths = { "salon" })
    Page<Pedido> findAllByOrderByCreadoEnDesc(Pageable pageable);

    List<Pedido> findByCreadoEnBetweenOrderByCreadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Pedido> findByFinalizadoEnBetweenOrderByFinalizadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    long countByEstadoIn(List<EstadoPedido> estados);
}
