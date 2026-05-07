package com.casaverde.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByInsumo(String insumo);
    List<Inventario> findAllByOrderByInsumoAsc();
    boolean existsByInsumo(String insumo);
}
