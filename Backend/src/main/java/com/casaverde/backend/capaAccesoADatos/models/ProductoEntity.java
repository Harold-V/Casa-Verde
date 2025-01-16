package com.casaverde.backend.capaAccesoADatos.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PedidoProductoEntity> pedidos = new ArrayList<>();
}
