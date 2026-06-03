package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
        List<MovimientoInventario> findByInsumoOrderByRegistradoEnDesc(String insumo);

        List<MovimientoInventario> findAllByOrderByRegistradoEnDesc();

        List<MovimientoInventario> findByRegistradoEnBetweenOrderByRegistradoEnDesc(
                        LocalDateTime desde, LocalDateTime hasta);

        List<MovimientoInventario> findByInsumoAndRegistradoEnBetweenOrderByRegistradoEnDesc(
                        String insumo, LocalDateTime desde, LocalDateTime hasta);

        @Query(value = """
                SELECT insumo,
                       CAST(strftime('%m', registrado_en) AS INTEGER) AS mes,
                       SUM(cantidad) AS total
                FROM movimiento_inventario
                WHERE tipo = 'salida'
                  AND referencia_tipo = 'pedido'
                  AND strftime('%Y', registrado_en) = :ano
                GROUP BY insumo, strftime('%m', registrado_en)
                ORDER BY mes
                """, nativeQuery = true)
        List<Object[]> sumaSalidasPorMesYInsumo(@Param("ano") String ano);
}
