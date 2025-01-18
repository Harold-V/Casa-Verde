package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ConfiguracionEntity;

@Repository
public interface ConfiguracionRepository extends CrudRepository<ConfiguracionEntity, String> {
    @Query("SELECT p.pedID FROM PedidoEntity p")
    List<Long> findAllProductIds();
}
