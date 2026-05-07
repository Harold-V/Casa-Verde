package com.casaverde.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.dto.SalonDTO;
import com.casaverde.backend.service.SalonService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/salones")
@RequiredArgsConstructor
public class SalonController {

    private final SalonService service;

    @GetMapping
    public List<SalonDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return soloActivos ? service.listarActivos() : service.listarTodos();
    }

    @GetMapping("/{id}")
    public SalonDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<SalonDTO> crear(@RequestBody SalonDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public SalonDTO actualizar(@PathVariable Long id, @RequestBody SalonDTO dto) {
        return service.actualizar(id, dto);
    }

    @PutMapping("/{id}/toggle")
    public SalonDTO toggleActivo(@PathVariable Long id) {
        return service.toggleActivo(id);
    }
}
