package com.casaverde.backend.capaAccesoADatos.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Configuracion")
public class ConfiguracionEntity {

    @Id
    @Column(nullable = false, length = 255)
    private String configClave;

    @Column(nullable = false, length = 255)
    private String configValor;
}
