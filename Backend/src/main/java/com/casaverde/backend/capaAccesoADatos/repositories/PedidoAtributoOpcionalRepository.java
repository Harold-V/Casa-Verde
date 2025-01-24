package com.casaverde.backend.capaAccesoADatos.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoAtributoOpacionalEntity;

@Repository
public interface PedidoAtributoOpcionalRepository
                extends
                CrudRepository<PedidoAtributoOpacionalEntity, PedidoAtributoOpacionalEntity.PedidoAtributoOpcionalKey> {

}
