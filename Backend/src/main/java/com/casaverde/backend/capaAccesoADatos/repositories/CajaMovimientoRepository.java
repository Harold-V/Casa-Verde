package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;

import java.math.BigDecimal;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {
       List<CajaMovimiento> findAllByOrderByRegistradoEnDesc();

       List<CajaMovimiento> findByRegistradoEnBetweenOrderByRegistradoEnDesc(LocalDateTime inicio, LocalDateTime fin);

       boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);

       /**
        * Suma el valor absoluto de todas las salidas (monto &lt; 0) de un fondo en el
        * rango dado.
        */
       @Query("SELECT COALESCE(SUM(ABS(c.monto)), 0) FROM CajaMovimiento c " +
                     "WHERE c.moneda = :fondoMoneda AND c.monto < 0 " +
                     "AND c.registradoEn >= :inicio AND c.registradoEn < :fin")
       BigDecimal sumSalidasFondoEnMes(@Param("fondoMoneda") String fondoMoneda,
                     @Param("inicio") LocalDateTime inicio,
                     @Param("fin") LocalDateTime fin);
}
