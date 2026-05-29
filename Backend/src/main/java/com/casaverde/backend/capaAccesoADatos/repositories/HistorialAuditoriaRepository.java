package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.HistorialAuditoria;

public interface HistorialAuditoriaRepository extends JpaRepository<HistorialAuditoria, Long> {
    List<HistorialAuditoria> findByEntidadAndEntidadIdOrderByRegistradoEnDesc(String entidad, Long entidadId);

    List<HistorialAuditoria> findAllByOrderByRegistradoEnDesc();
}
