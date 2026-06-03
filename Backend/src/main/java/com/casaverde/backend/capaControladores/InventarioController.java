package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.ComparativoMensualDTO;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;
import com.casaverde.backend.fachadaServices.services.productoService.IInventarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final IInventarioService service;

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
    public ResponseEntity<MovimientoInventarioDTO> registrarMovimiento(@Valid @RequestBody MovimientoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarMovimiento(req));
    }

    @PatchMapping("/{insumo}/alerta")
    public InventarioDTO actualizarAlerta(
            @PathVariable String insumo,
            @RequestBody Map<String, BigDecimal> body) {
        return service.actualizarAlerta(insumo, body.get("alertaMinima"));
    }

    @GetMapping("/comparativo")
    public ComparativoMensualDTO comparativoAnual(
            @RequestParam(required = false) Integer año) {
        int anio = año != null ? año : LocalDate.now().getYear();
        return service.comparativoAnual(anio);
    }
}
