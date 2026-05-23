package com.casaverde.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.HistorialAuditoria;
import com.casaverde.backend.dto.HistorialAuditoriaDTO;
import com.casaverde.backend.repository.HistorialAuditoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final HistorialAuditoriaRepository repo;

    @Transactional(readOnly = true)
    public List<HistorialAuditoriaDTO> listar(String entidad, Long entidadId) {
        if (entidad != null && !entidad.isBlank() && entidadId != null) {
            return repo.findByEntidadAndEntidadIdOrderByRegistradoEnDesc(entidad.trim(), entidadId)
                    .stream().map(this::toDTO).collect(Collectors.toList());
        }
        return repo.findAllByOrderByRegistradoEnDesc()
                .stream()
                .filter(a -> entidad == null || entidad.isBlank() || entidad.trim().equals(a.getEntidad()))
                .filter(a -> entidadId == null || entidadId.equals(a.getEntidadId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void registrar(String entidad, Long entidadId, String accion, String detalle) {
        repo.save(HistorialAuditoria.builder()
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .detalle(detalle)
                .build());
    }

    private HistorialAuditoriaDTO toDTO(HistorialAuditoria auditoria) {
        return HistorialAuditoriaDTO.builder()
                .id(auditoria.getId())
                .entidad(auditoria.getEntidad())
                .entidadId(auditoria.getEntidadId())
                .accion(auditoria.getAccion())
                .detalle(auditoria.getDetalle())
                .usuario(auditoria.getUsuario())
                .registradoEn(auditoria.getRegistradoEn())
                .build();
    }
}
