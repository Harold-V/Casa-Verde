package com.casaverde.backend.service;

import java.math.BigDecimal;
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
    private final MonetaryService monetaryService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public TasaCambioDTO obtenerVigente() {
        return repo.findVigente()
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay tasa de cambio registrada"));
    }

    @Transactional(readOnly = true)
    public List<TasaCambioDTO> historial() {
        return repo.findAllByOrderByVigenteDesdeDesc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TasaCambioDTO registrar(BigDecimal valorCop) {
        valorCop = monetaryService.normalizarTasa(valorCop);
        TasaCambio tasa = TasaCambio.builder().valorCop(valorCop).build();
        TasaCambio guardada = repo.save(tasa);
        auditoriaService.registrar("tasa_cambio", guardada.getId(), "REGISTRAR",
                "Nueva tasa USD/COP " + valorCop);
        return toDTO(guardada);
    }

    private TasaCambioDTO toDTO(TasaCambio t) {
        return TasaCambioDTO.builder()
                .id(t.getId())
                .valorCop(t.getValorCop())
                .vigenteDesde(t.getVigenteDesde())
                .creadoEn(t.getCreadoEn())
                .build();
    }
}
