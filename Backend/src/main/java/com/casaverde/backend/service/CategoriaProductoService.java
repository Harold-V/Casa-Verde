package com.casaverde.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.CategoriaProducto;
import com.casaverde.backend.dto.CategoriaProductoDTO;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.CategoriaProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaProductoService {

    private final CategoriaProductoRepository repo;

    public List<CategoriaProductoDTO> listarTodas() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<CategoriaProductoDTO> listarActivas() {
        return repo.findByActivoTrueOrderByOrdenAsc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CategoriaProductoDTO buscarPorId(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional
    public CategoriaProductoDTO crear(CategoriaProductoDTO dto) {
        if (repo.existsByNombre(dto.getNombre())) {
            throw new ValidacionException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }
        CategoriaProducto entity = CategoriaProducto.builder()
                .nombre(dto.getNombre())
                .orden(dto.getOrden() != null ? dto.getOrden() : 0)
                .activo(true)
                .build();
        return toDTO(repo.save(entity));
    }

    @Transactional
    public CategoriaProductoDTO actualizar(Long id, CategoriaProductoDTO dto) {
        CategoriaProducto entity = findOrThrow(id);
        if (repo.existsByNombreAndIdNot(dto.getNombre(), id)) {
            throw new ValidacionException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }
        entity.setNombre(dto.getNombre());
        if (dto.getOrden() != null) entity.setOrden(dto.getOrden());
        if (dto.getActivo() != null) entity.setActivo(dto.getActivo());
        return toDTO(repo.save(entity));
    }

    @Transactional
    public void toggleActivo(Long id) {
        CategoriaProducto entity = findOrThrow(id);
        entity.setActivo(!entity.getActivo());
        repo.save(entity);
    }

    private CategoriaProducto findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + id));
    }

    private CategoriaProductoDTO toDTO(CategoriaProducto e) {
        return CategoriaProductoDTO.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .orden(e.getOrden())
                .activo(e.getActivo())
                .build();
    }
}
