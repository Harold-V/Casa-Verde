package com.casaverde.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.TasaCambio;
import com.casaverde.backend.dto.TasaCambioDTO;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.TasaCambioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TasaCambioService {

    private final TasaCambioRepository repo;

    public TasaCambioDTO obtenerVigente() {
        return repo.findVigente()
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay tasa de cambio registrada"));
    }

    public List<TasaCambioDTO> historial() {
        return repo.findAllByOrderByVigentDesdeDesc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TasaCambioDTO registrar(Double valorCop) {
        if (valorCop == null || valorCop <= 0) {
            throw new ValidacionException("La tasa debe ser mayor a 0");
        }
        TasaCambio tasa = TasaCambio.builder().valorCop(valorCop).build();
        return toDTO(repo.save(tasa));
    }

    private TasaCambioDTO toDTO(TasaCambio t) {
        return TasaCambioDTO.builder()
                .id(t.getId())
                .valorCop(t.getValorCop())
                .vigentDesde(t.getVigentDesde())
                .creadoEn(t.getCreadoEn())
                .build();
    }
}
