package com.casaverde.backend.fachadaServices.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PagoEntity;
import com.casaverde.backend.fachadaServices.DTO.PagoDTO;

@Configuration
public class mapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Configura el mapeo manual para PagoEntity -> PagoDTO
        modelMapper.typeMap(PagoEntity.class, PagoDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getPedido().getPedID(), PagoDTO::setPedID);
        });

        return modelMapper;
    }
}