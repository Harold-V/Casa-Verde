package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.ProductoAdicionalEntity;


@Repository
public interface ProductoAdicionalRepository extends CrudRepository<ProductoAdicionalEntity, Long> {

    @Query("SELECT p.prodAdID FROM ProductoAdicionalEntity p")
    List<Long> findAllProductIds();
}
