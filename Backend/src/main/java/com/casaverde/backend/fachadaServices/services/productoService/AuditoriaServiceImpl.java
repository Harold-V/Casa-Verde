package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    @Override
    @Transactional(readOnly = true)
    public Page<HistorialAuditoriaDTO> listarPaginado(String entidad, Long entidadId, Pageable pageable) {
        if (entidad != null && !entidad.isBlank() && entidadId != null) {
            return repo.findByEntidadAndEntidadIdOrderByRegistradoEnDesc(entidad.trim(), entidadId, pageable)
                    .map(auditoriaMapper::toDTO);
        }
        if ((entidad == null || entidad.isBlank()) && entidadId == null) {
            return repo.findAllByOrderByRegistradoEnDesc(pageable)
                    .map(auditoriaMapper::toDTO);
        }
        return paginarLista(listar(entidad, entidadId), pageable);
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

    private <T> Page<T> paginarLista(List<T> lista, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), lista.size());
        int end = Math.min(start + pageable.getPageSize(), lista.size());
        return new PageImpl<>(lista.subList(start, end), pageable, lista.size());
    }

}
