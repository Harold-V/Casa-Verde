package com.casaverde.backend.capaAccesoADatos.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) // name no puede ser nulo
    private String name;

    @Column(name = "last_name") // Mapeo de columna personalizada
    private String lastName;

    @Column(nullable = false) // age no puede ser nulo
    private Integer age;

    @Column(nullable = false, unique = true) // email único y no nulo
    private String email;

    public UserEntity() {
    }
}
