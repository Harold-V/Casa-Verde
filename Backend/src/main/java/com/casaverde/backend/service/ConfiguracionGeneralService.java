package com.casaverde.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.ConfiguracionGeneral;
import com.casaverde.backend.dto.ConfiguracionDTO;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.ConfiguracionGeneralRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionGeneralService {

    private final ConfiguracionGeneralRepository repo;

    @Transactional(readOnly = true)
    public List<ConfiguracionDTO> listarTodas() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConfiguracionDTO buscarPorClave(String clave) {
        return toDTO(findOrThrow(clave));
    }

    @Transactional
    public ConfiguracionDTO guardar(ConfiguracionDTO dto) {
        if (dto.getClave() == null || dto.getClave().isBlank()) {
            throw new ValidacionException("La clave de configuración es obligatoria");
        }
        ConfiguracionGeneral config = repo.findById(dto.getClave())
                .orElse(new ConfiguracionGeneral());
        config.setClave(dto.getClave().trim());
        config.setValor(dto.getValor() != null ? dto.getValor() : "");
        config.setDescripcion(dto.getDescripcion());
        return toDTO(repo.save(config));
    }

    @Transactional
    public ConfiguracionDTO actualizarValor(String clave, String valor) {
        ConfiguracionGeneral config = findOrThrow(clave);
        config.setValor(valor != null ? valor : "");
        return toDTO(repo.save(config));
    }

    @Transactional
    public void eliminar(String clave) {
        if (!repo.existsById(clave)) {
            throw new RecursoNoEncontradoException("Configuración no encontrada: " + clave);
        }
        repo.deleteById(clave);
    }

    private ConfiguracionGeneral findOrThrow(String clave) {
        return repo.findById(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración no encontrada: " + clave));
    }

    private ConfiguracionDTO toDTO(ConfiguracionGeneral c) {
        return ConfiguracionDTO.builder()
                .clave(c.getClave())
                .valor(c.getValor())
                .descripcion(c.getDescripcion())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }
}
