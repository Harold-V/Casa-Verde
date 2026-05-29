package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;

import com.casaverde.backend.fachadaServices.dto.ActualizarDetalleRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.services.pedidoService.IPedidoService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final IPedidoService service;

    @GetMapping
    public List<PedidoDTO> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? service.listarTodos() : service.listarActivos();
    }

    @GetMapping("/{id}")
    public PedidoDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping(value = "/{id}/comprobante", produces = MediaType.TEXT_PLAIN_VALUE)
    public String comprobante(@PathVariable Long id) {
        return service.comprobanteTexto(id);
    }

    @PostMapping
    public ResponseEntity<PedidoDTO> crear(@Valid @RequestBody CrearPedidoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PatchMapping("/{id}")
    public PedidoDTO actualizarPedido(@PathVariable Long id, @Valid @RequestBody ActualizarPedidoRequest req) {
        return service.actualizarPedido(id, req);
    }

    @PostMapping("/{id}/detalles")
    public PedidoDTO agregarDetalle(@PathVariable Long id, @Valid @RequestBody DetalleItemRequest req) {
        return service.agregarDetalle(id, req);
    }

    @PatchMapping("/{id}/detalles/{detalleId}")
    public PedidoDTO actualizarDetalle(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @Valid @RequestBody ActualizarDetalleRequest req) {
        return service.actualizarDetalle(id, detalleId, req);
    }

    @DeleteMapping("/{id}/detalles/{detalleId}")
    public PedidoDTO quitarDetalle(@PathVariable Long id, @PathVariable Long detalleId) {
        return service.quitarDetalle(id, detalleId);
    }

    @PostMapping("/{id}/pagos")
    public PedidoDTO registrarPago(@PathVariable Long id, @Valid @RequestBody RegistrarPagoRequest req) {
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
            @Valid @RequestBody ActualizarPagoRequest req) {
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
