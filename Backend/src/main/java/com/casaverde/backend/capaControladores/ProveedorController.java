package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;
import com.casaverde.backend.fachadaServices.services.compraService.IProveedorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final IProveedorService proveedorService;

    @GetMapping
    public List<ProveedorDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return proveedorService.listar(soloActivos);
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> crear(@RequestBody ProveedorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(req));
    }

    @PutMapping("/{id}")
    public ProveedorDTO actualizar(@PathVariable Long id, @RequestBody ProveedorRequest req) {
        return proveedorService.actualizar(id, req);
    }

    @PutMapping("/{id}/toggle")
    public ProveedorDTO toggle(@PathVariable Long id) {
        return proveedorService.toggle(id);
    }
}
