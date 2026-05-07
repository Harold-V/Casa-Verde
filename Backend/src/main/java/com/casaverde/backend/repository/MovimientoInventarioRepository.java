package com.casaverde.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByInsumoOrderByRegistradoEnDesc(String insumo);
    List<MovimientoInventario> findAllByOrderByRegistradoEnDesc();
    List<MovimientoInventario> findByRegistradoEnBetweenOrderByRegistradoEnDesc(
            LocalDateTime desde, LocalDateTime hasta);
    List<MovimientoInventario> findByInsumoAndRegistradoEnBetweenOrderByRegistradoEnDesc(
            String insumo, LocalDateTime desde, LocalDateTime hasta);
}
