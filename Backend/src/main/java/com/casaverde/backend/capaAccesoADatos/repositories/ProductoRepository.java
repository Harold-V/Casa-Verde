package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoEntity;

@Repository
public interface ProductoRepository extends CrudRepository<ProductoEntity, Long> {

    @Query("SELECT p.prodID FROM ProductoEntity p")
    List<Long> findAllProductIds();
}
