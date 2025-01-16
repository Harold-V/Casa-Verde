package com.casaverde.backend.capaAccesoADatos.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Producto") // Nombre de la tabla en la base de datos
public class ProductoEntity {

    @Id
    private Long prodID;

    @Column(nullable = false) // prodNombre no puede ser nulo
    private String prodNombre;

    @Column(nullable = false) // prodPrecio no puede ser nulo
    private Double prodPrecio;

}
