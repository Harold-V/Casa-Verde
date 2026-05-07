package com.casaverde.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.Salon;

public interface SalonRepository extends JpaRepository<Salon, Long> {
    List<Salon> findByActivoTrueOrderByNombreAsc();
    List<Salon> findAllByOrderByNombreAsc();
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
}
