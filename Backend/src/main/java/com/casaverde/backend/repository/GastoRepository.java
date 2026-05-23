package com.casaverde.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.domain.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, Long> {
    List<Gasto> findByRegistradoEnBetweenOrderByRegistradoEnDesc(LocalDateTime inicio, LocalDateTime fin);
    List<Gasto> findAllByOrderByRegistradoEnDesc();
}
