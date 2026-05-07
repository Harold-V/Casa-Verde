package com.casaverde.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.ConfiguracionGeneral;

public interface ConfiguracionGeneralRepository extends JpaRepository<ConfiguracionGeneral, String> {
}
