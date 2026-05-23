package com.casaverde.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.dto.*;
import com.casaverde.backend.service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @GetMapping
    public List<PedidoDTO> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? service.listarTodos() : service.listarActivos();
    }

    @GetMapping("/{id}")
    public PedidoDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PedidoDTO> crear(@RequestBody CrearPedidoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PatchMapping("/{id}")
    public PedidoDTO actualizarPedido(@PathVariable Long id, @RequestBody ActualizarPedidoRequest req) {
        return service.actualizarPedido(id, req);
    }

    @PostMapping("/{id}/detalles")
    public PedidoDTO agregarDetalle(@PathVariable Long id, @RequestBody DetalleItemRequest req) {
        return service.agregarDetalle(id, req);
    }

    @PatchMapping("/{id}/detalles/{detalleId}")
    public PedidoDTO actualizarDetalle(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @RequestBody ActualizarDetalleRequest req) {
        return service.actualizarDetalle(id, detalleId, req);
    }

    @DeleteMapping("/{id}/detalles/{detalleId}")
    public PedidoDTO quitarDetalle(@PathVariable Long id, @PathVariable Long detalleId) {
        return service.quitarDetalle(id, detalleId);
    }

    @PostMapping("/{id}/pagos")
    public PedidoDTO registrarPago(@PathVariable Long id, @RequestBody RegistrarPagoRequest req) {
        return service.registrarPago(id, req);
    }

    @DeleteMapping("/{id}/pagos/{pagoId}")
    public PedidoDTO quitarPago(@PathVariable Long id, @PathVariable Long pagoId) {
        return service.quitarPago(id, pagoId);
    }

    @PatchMapping("/{id}/pagos/{pagoId}")
    public PedidoDTO actualizarPago(
            @PathVariable Long id,
            @PathVariable Long pagoId,
            @RequestBody ActualizarPagoRequest req) {
        return service.actualizarPago(id, pagoId, req);
    }

    @PutMapping("/{id}/finalizar")
    public PedidoDTO finalizar(@PathVariable Long id) {
        return service.finalizarPedido(id);
    }

    @PutMapping("/{id}/anular")
    public PedidoDTO anular(@PathVariable Long id) {
        return service.anularPedido(id);
    }
}
