package com.casaverde.backend.capaAccesoADatos.repositories;

import org.springframework.data.repository.CrudRepository;

import com.casaverde.backend.capaAccesoADatos.models.UserEntity;

public interface UserRepository extends CrudRepository<UserEntity, Long> {

}
