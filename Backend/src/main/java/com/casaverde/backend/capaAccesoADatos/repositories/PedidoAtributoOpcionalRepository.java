package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoAtributoOpcionalEntity;

@Repository
public interface PedidoAtributoOpcionalRepository extends CrudRepository<PedidoAtributoOpcionalEntity, String> {

    @Query("SELECT p.pedAtrOpcClave FROM PedidoAtributoOpcionalEntity p")
    List<Long> findAllProductIds();
}
