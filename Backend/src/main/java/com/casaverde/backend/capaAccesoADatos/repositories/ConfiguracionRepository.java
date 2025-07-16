package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.ConfiguracionEntity;

@Repository
public interface ConfiguracionRepository extends CrudRepository<ConfiguracionEntity, String> {
    @Query("SELECT p.configClave FROM ConfiguracionEntity p")
    List<Long> findAllProductIds();
}
