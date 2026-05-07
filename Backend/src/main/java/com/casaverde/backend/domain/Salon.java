package com.casaverde.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private Integer aforo;

    @Column(nullable = false)
    private Boolean activo = true;
}
