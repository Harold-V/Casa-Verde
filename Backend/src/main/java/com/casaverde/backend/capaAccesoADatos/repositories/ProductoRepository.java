package com.casaverde.backend.capaAccesoADatos.repositories;

import org.springframework.data.repository.CrudRepository;

import com.casaverde.backend.capaAccesoADatos.models.ProductoEntity;

public interface ProductoRepository extends CrudRepository<ProductoEntity, Long> {

}
