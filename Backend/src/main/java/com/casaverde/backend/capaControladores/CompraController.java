package com.casaverde.backend.capaControladores;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;
import com.casaverde.backend.fachadaServices.services.compraService.ICompraService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
public class CompraController {

    private final ICompraService compraService;

    @GetMapping
    public List<CompraDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoCompra estado) {
        return compraService.listar(fecha, estado);
    }

    @GetMapping("/{id}")
    public CompraDTO obtener(@PathVariable Long id) {
        return compraService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CompraDTO> registrar(@RequestBody CompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        compraService.anular(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/abonos")
    public List<AbonoCompraDTO> listarAbonos(@PathVariable Long id) {
        return compraService.listarAbonos(id);
    }

    @PostMapping("/{id}/abonos")
    public ResponseEntity<AbonoCompraDTO> registrarAbono(
            @PathVariable Long id,
            @RequestBody AbonoCompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrarAbono(id, req));
    }
}
