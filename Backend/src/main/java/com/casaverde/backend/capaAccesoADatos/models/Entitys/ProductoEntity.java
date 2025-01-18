package com.casaverde.backend.capaAccesoADatos.models.Entitys;

import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoProducto;
import com.casaverde.backend.capaAccesoADatos.models.Relations.PedidoProductoRelation;

import jakarta.persistence.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    private List<PedidoProductoRelation> pedidos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) //
    private EstadoProducto prodEstado;

}
