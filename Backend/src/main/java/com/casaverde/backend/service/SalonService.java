package com.casaverde.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.Salon;
import com.casaverde.backend.dto.SalonDTO;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.SalonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository repo;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<SalonDTO> listarTodos() {
        return repo.findAllByOrderByNombreAsc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalonDTO> listarActivos() {
        return repo.findByActivoTrueOrderByNombreAsc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalonDTO buscarPorId(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional
    public SalonDTO crear(SalonDTO dto) {
        validarNombre(dto.getNombre(), null);
        Salon salon = Salon.builder()
                .nombre(dto.getNombre().trim())
                .aforo(dto.getAforo())
                .activo(true)
                .build();
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "CREAR", "Salón creado: " + guardado.getNombre());
        return toDTO(guardado);
    }

    @Transactional
    public SalonDTO actualizar(Long id, SalonDTO dto) {
        Salon salon = findOrThrow(id);
        validarNombre(dto.getNombre(), id);
        salon.setNombre(dto.getNombre().trim());
        salon.setAforo(dto.getAforo());
        if (dto.getActivo() != null) salon.setActivo(dto.getActivo());
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "ACTUALIZAR", "Salón actualizado: " + guardado.getNombre());
        return toDTO(guardado);
    }

    @Transactional
    public SalonDTO toggleActivo(Long id) {
        Salon salon = findOrThrow(id);
        salon.setActivo(!salon.getActivo());
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "CAMBIAR_ESTADO", "Salón activo=" + guardado.getActivo());
        return toDTO(guardado);
    }

    private void validarNombre(String nombre, Long idExcluir) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidacionException("El nombre del salón es obligatorio");
        }
        boolean existe = idExcluir == null
                ? repo.existsByNombre(nombre.trim())
                : repo.existsByNombreAndIdNot(nombre.trim(), idExcluir);
        if (existe) {
            throw new ValidacionException("Ya existe un salón con el nombre: " + nombre.trim());
        }
    }

    private Salon findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + id));
    }

    private SalonDTO toDTO(Salon s) {
        return SalonDTO.builder()
                .id(s.getId()).nombre(s.getNombre())
                .aforo(s.getAforo()).activo(s.getActivo())
                .build();
    }
}
