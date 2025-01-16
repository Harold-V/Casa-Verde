package com.casaverde.backend.fachadaServices.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class mapperConfig {
    @Bean
    ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
