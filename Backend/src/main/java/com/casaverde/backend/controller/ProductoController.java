package com.casaverde.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.dto.ProductoDTO;
import com.casaverde.backend.dto.ProductoRequest;
import com.casaverde.backend.service.ProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping
    public List<ProductoDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return soloActivos ? service.listarActivos() : service.listarTodos();
    }

    @GetMapping("/{id}")
    public ProductoDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @PutMapping("/{id}/precio")
    public ProductoDTO actualizarPrecio(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        return service.actualizarPrecio(id, body.get("precio"));
    }

    @PutMapping("/{id}/toggle")
    public ProductoDTO toggleActivo(@PathVariable Long id) {
        return service.toggleActivo(id);
    }
}
