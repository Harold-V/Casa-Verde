package com.casaverde.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.Fondo;

public interface FondoRepository extends JpaRepository<Fondo, String> {
}
