package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoEntity;

@Repository
public interface PedidoRepository extends CrudRepository<PedidoEntity, Long> {

    @Query("SELECT p.pedID FROM PedidoEntity p")
    List<Long> findAllPedidoIds();

    @Modifying
    @Query("DELETE FROM PedidoProductoRelation r WHERE r.pedido.pedID = :pedidoId AND r.producto.prodID NOT IN :nuevosIds")
    void eliminarRelacionesObsoletas(@Param("pedidoId") Long pedidoId, @Param("nuevosIds") List<Long> nuevosIds);

}
