package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.PagoEntity;

@Repository
public interface PagoRepository extends CrudRepository<PagoEntity, Long> {

    @Query("SELECT p.pagoID FROM PagoEntity p")
    List<Long> findAllPagoIds();
}