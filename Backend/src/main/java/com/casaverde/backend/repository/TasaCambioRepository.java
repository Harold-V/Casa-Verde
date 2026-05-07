package com.casaverde.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.casaverde.backend.domain.TasaCambio;

public interface TasaCambioRepository extends JpaRepository<TasaCambio, Long> {

    @Query("SELECT t FROM TasaCambio t ORDER BY t.vigentDesde DESC LIMIT 1")
    Optional<TasaCambio> findVigente();

    List<TasaCambio> findAllByOrderByVigentDesdeDesc();
}
