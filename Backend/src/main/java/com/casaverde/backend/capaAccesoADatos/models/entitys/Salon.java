package com.casaverde.backend.capaAccesoADatos.models.entitys;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salon", indexes = {
        @Index(name = "idx_salon_activo_eliminado", columnList = "activo, eliminado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    private Integer aforo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT 0")
    @Builder.Default
    private Boolean eliminado = false;
}
