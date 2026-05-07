package com.casaverde.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.CategoriaProducto;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {
    List<CategoriaProducto> findByActivoTrueOrderByOrdenAsc();
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
}
