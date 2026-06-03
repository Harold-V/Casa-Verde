package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbonoCompraRepository extends JpaRepository<AbonoCompra, Long> {
    List<AbonoCompra> findByCompraIdOrderByCreadoEnAsc(Long compraId);
}
