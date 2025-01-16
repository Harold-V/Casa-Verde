package com.casaverde.backend.capaAccesoADatos.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "ProductoAdicional") // Nombre de la tabla en la base de datos
public class ProductoAdicionalEntity {

    @Id
    private Long prodAdID;

    @Column(nullable = false) // prodAdNombre no puede ser nulo
    private String prodAdNombre;

    @Column(nullable = false) // prodAdPrecio no puede ser nulo
    private Double prodAdPrecio;

    // Constructor sin parámetros (requerido por JPA)
    public ProductoAdicionalEntity() {
    }
}
