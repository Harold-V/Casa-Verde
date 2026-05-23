package com.casaverde.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.casaverde.backend.domain.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByRegistradoEnBetweenOrderByRegistradoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT p FROM Pago p LEFT JOIN FETCH p.pedido WHERE p.registradoEn >= :inicio AND p.registradoEn < :fin ORDER BY p.registradoEn DESC")
    List<Pago> findWithPedidoByRegistradoEnBetween(
            @org.springframework.data.repository.query.Param("inicio") LocalDateTime inicio,
            @org.springframework.data.repository.query.Param("fin") LocalDateTime fin);

    @Query("SELECT p FROM Pago p LEFT JOIN FETCH p.pedido ORDER BY p.registradoEn DESC")
    List<Pago> findAllConPedidoOrderByRegistradoEnDesc();

    @Query("SELECT p FROM Pago p WHERE p.registradoEn >= :inicio AND p.registradoEn < :fin ORDER BY p.registradoEn DESC")
    List<Pago> findByDia(@org.springframework.data.repository.query.Param("inicio") LocalDateTime inicio,
                         @org.springframework.data.repository.query.Param("fin") LocalDateTime fin);
}
