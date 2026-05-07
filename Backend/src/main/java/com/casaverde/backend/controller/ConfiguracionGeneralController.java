package com.casaverde.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.dto.ConfiguracionDTO;
import com.casaverde.backend.service.ConfiguracionGeneralService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionGeneralController {

    private final ConfiguracionGeneralService service;

    @GetMapping
    public List<ConfiguracionDTO> listar() {
        return service.listarTodas();
    }

    @GetMapping("/{clave}")
    public ConfiguracionDTO buscar(@PathVariable String clave) {
        return service.buscarPorClave(clave);
    }

    @PostMapping
    public ConfiguracionDTO guardar(@RequestBody ConfiguracionDTO dto) {
        return service.guardar(dto);
    }

    @PutMapping("/{clave}")
    public ConfiguracionDTO actualizarValor(@PathVariable String clave, @RequestBody Map<String, String> body) {
        return service.actualizarValor(clave, body.get("valor"));
    }

    @DeleteMapping("/{clave}")
    public ResponseEntity<Void> eliminar(@PathVariable String clave) {
        service.eliminar(clave);
        return ResponseEntity.noContent().build();
    }
}
