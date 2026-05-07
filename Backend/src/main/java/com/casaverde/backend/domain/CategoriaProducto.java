package com.casaverde.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_producto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;
}
