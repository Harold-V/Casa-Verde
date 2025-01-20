package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoPresasEntity;

@Repository
public interface ProductoPresasRepository
        extends CrudRepository<ProductoPresasEntity, ProductoPresasEntity.ProductoPresasKey> {

    List<ProductoPresasEntity> findByProductoProdID(Long prodID);

    @Query("SELECT p FROM ProductoPresasEntity p WHERE p.cantidad = :cantidad AND p.id.recurso = :recurso")
    List<ProductoPresasEntity> findByCantidadAndRecurso(@Param("cantidad") Integer cantidad,
            @Param("recurso") String recurso);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductoPresasEntity p WHERE p.producto.prodID = :prodID")
    void deleteAllByProductoProdID(Long prodID);

}
