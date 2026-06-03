package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findAllByOrderByFechaDescCreadoEnDesc();
    List<Compra> findByProveedorIdOrderByFechaDescCreadoEnDesc(Long proveedorId);
}
