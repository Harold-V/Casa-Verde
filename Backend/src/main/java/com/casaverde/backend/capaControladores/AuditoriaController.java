package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final IAuditoriaService service;

    @GetMapping
    public List<HistorialAuditoriaDTO> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId) {
        return service.listar(entidad, entidadId);
    }
}
