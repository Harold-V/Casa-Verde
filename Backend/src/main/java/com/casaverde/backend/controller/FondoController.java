package com.casaverde.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.dto.AjusteCapitalRequest;
import com.casaverde.backend.dto.FondoDTO;
import com.casaverde.backend.dto.MovimientoFondoDTO;
import com.casaverde.backend.service.FondoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/fondos")
@RequiredArgsConstructor
public class FondoController {

    private final FondoService service;

    @GetMapping
    public List<FondoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/historial")
    public List<MovimientoFondoDTO> historial() {
        return service.historial();
    }

    @PutMapping("/{moneda}")
    public FondoDTO ajustar(@PathVariable String moneda, @RequestBody AjusteCapitalRequest req) {
        return service.ajustar(moneda.toUpperCase(), req);
    }
}
