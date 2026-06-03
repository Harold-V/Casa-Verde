package com.casaverde.backend.fachadaServices.dto;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorRequest {
    private String nombre;
    private TipoProveedor tipo;
    private String telefono;
}
