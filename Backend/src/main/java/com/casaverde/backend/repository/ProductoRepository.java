package com.casaverde.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrueOrderByNombreAsc();
    List<Producto> findAllByOrderByNombreAsc();
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
    List<Producto> findByCategoriaId(Long categoriaId);
}
