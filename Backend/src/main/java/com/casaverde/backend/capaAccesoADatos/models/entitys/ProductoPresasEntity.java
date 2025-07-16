package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ProductoPresas")
public class ProductoPresasEntity {

    @EmbeddedId
    private ProductoPresasKey id;

    @ManyToOne
    @MapsId("prodID")
    @JoinColumn(name = "prodID", nullable = false)
    private ProductoEntity producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Embeddable
    public static class ProductoPresasKey implements Serializable {

        @Column(name = "prodID")
        private Long prodID;

        @Column(name = "recurso")
        private String recurso;

    }
}
