package com.casaverde.backend.fachadaServices.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoPresasEntity;
import com.casaverde.backend.fachadaServices.DTO.ProductoPresasDTO;

@Configuration
public class mapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Configura el mapeo manual para ProductoPresasEntity -> ProductoPresasDTO
        modelMapper.typeMap(ProductoPresasEntity.class, ProductoPresasDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getId().getRecurso(), ProductoPresasDTO::setRecurso);
            mapper.map(ProductoPresasEntity::getCantidad, ProductoPresasDTO::setCantidad);
        });

        return modelMapper;
    }
}
