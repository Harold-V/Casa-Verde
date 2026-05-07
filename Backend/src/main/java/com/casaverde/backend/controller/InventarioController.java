package com.casaverde.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.dto.InventarioDTO;
import com.casaverde.backend.dto.MovimientoInventarioDTO;
import com.casaverde.backend.dto.MovimientoRequest;
import com.casaverde.backend.service.InventarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService service;

    @GetMapping
    public List<InventarioDTO> listarStock() {
        return service.listarStock();
    }

    @GetMapping("/{insumo}")
    public InventarioDTO obtenerStock(@PathVariable String insumo) {
        return service.obtenerStock(insumo);
    }

    @GetMapping("/movimientos")
    public List<MovimientoInventarioDTO> listarMovimientos(
            @RequestParam(required = false) String insumo) {
        return insumo != null
                ? service.listarMovimientosPorInsumo(insumo)
                : service.listarMovimientos();
    }

    @PostMapping("/movimiento")
    public ResponseEntity<MovimientoInventarioDTO> registrarMovimiento(@RequestBody MovimientoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarMovimiento(req));
    }
}
