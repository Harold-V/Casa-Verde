package com.casaverde.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.CajaMovimiento;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {
    List<CajaMovimiento> findAllByOrderByRegistradoEnDesc();
    boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);
}
