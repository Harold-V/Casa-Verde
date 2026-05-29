package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.HistorialAuditoria;
import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;
import com.casaverde.backend.fachadaServices.mapper.AuditoriaMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.HistorialAuditoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements IAuditoriaService {

    private final HistorialAuditoriaRepository repo;
    private final AuditoriaMapper auditoriaMapper;

    @Transactional(readOnly = true)
    public List<HistorialAuditoriaDTO> listar(String entidad, Long entidadId) {
        if (entidad != null && !entidad.isBlank() && entidadId != null) {
            return repo.findByEntidadAndEntidadIdOrderByRegistradoEnDesc(entidad.trim(), entidadId)
                    .stream().map(auditoriaMapper::toDTO).collect(Collectors.toList());
        }
        return repo.findAllByOrderByRegistradoEnDesc()
                .stream()
                .filter(a -> entidad == null || entidad.isBlank() || entidad.trim().equals(a.getEntidad()))
                .filter(a -> entidadId == null || entidadId.equals(a.getEntidadId()))
                .map(auditoriaMapper::toDTO)
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

}
