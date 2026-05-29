package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.EntradaFondoRequest;
import com.casaverde.backend.fachadaServices.dto.EstadoNequiMesDTO;
import com.casaverde.backend.fachadaServices.dto.FondoDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoFondoDTO;
import com.casaverde.backend.fachadaServices.dto.TransferenciaFondoRequest;
import com.casaverde.backend.fachadaServices.services.pagoService.IFondoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/fondos")
@RequiredArgsConstructor
public class FondoController {

    private final IFondoService service;

    @GetMapping
    public List<FondoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/historial")
    public List<MovimientoFondoDTO> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String tipo) {
        return service.historial(fecha, moneda, tipo);
    }

    @PutMapping("/{moneda}")
    public FondoDTO ajustar(@PathVariable String moneda, @Valid @RequestBody AjusteCapitalRequest req) {
        return service.ajustar(moneda, req);
    }

    @PostMapping("/entrada")
    public FondoDTO registrarEntrada(@Valid @RequestBody EntradaFondoRequest req) {
        return service.registrarEntrada(req);
    }

    @PostMapping("/transferencia")
    public void registrarTransferencia(@Valid @RequestBody TransferenciaFondoRequest req) {
        service.registrarTransferencia(req);
    }

    @GetMapping("/nequi/estado-mes")
    public EstadoNequiMesDTO estadoNequiMes(@RequestParam(required = false) String fondo) {
        return service.estadoNequiMes(fondo);
    }

    @PostMapping("/nequi/cuentas")
    public FondoDTO crearCuentaNequi(@RequestBody Map<String, String> body) {
        return service.crearCuentaNequi(body.get("alias"));
    }

    @DeleteMapping("/nequi/cuentas/{moneda}")
    public ResponseEntity<Void> eliminarCuentaNequi(@PathVariable String moneda) {
        service.eliminarCuentaNequi(moneda);
        return ResponseEntity.noContent().build();
    }
}
