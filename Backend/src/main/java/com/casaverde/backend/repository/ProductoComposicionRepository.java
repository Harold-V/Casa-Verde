package com.casaverde.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.ProductoComposicionInventario;

public interface ProductoComposicionRepository extends JpaRepository<ProductoComposicionInventario, Long> {
    List<ProductoComposicionInventario> findByProductoId(Long productoId);
    void deleteByProductoId(Long productoId);
}
