package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoEntity;

@Repository
public interface ProductoRepository extends CrudRepository<ProductoEntity, Long> {

    Optional<ProductoEntity> findByProdNombre(String prodNombre);

    boolean existsByProdNombre(String prodNombre);

    boolean existsByProdNombreAndProdIDNot(String prodNombre, Long id);

    @Query("SELECT p.prodID FROM ProductoEntity p")
    List<Long> findAllProductIds();
}
