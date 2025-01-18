package com.casaverde.backend.fachadaServices.services.ConfiguracionService;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ConfiguracionEntity;
import com.casaverde.backend.capaAccesoADatos.repositories.ConfiguracionRepository;
import com.casaverde.backend.fachadaServices.DTO.ConfiguracionDTO;

@Service
public class ConfiguracionServiceImpl implements IConfiguracionService {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionServiceImpl.class);

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Método privado para convertir de Entity a DTO
    private ConfiguracionDTO convertToDTO(ConfiguracionEntity configuracionEntity) {
        return modelMapper.map(configuracionEntity, ConfiguracionDTO.class);
    }

    // Método privado para convertir de DTO a Entity
    private ConfiguracionEntity convertToEntity(ConfiguracionDTO configuracionDTO) {
        return modelMapper.map(configuracionDTO, ConfiguracionEntity.class);
    }

    @Override
    public List<ConfiguracionDTO> findAll() {
        List<ConfiguracionDTO> configuracionDTOs = new ArrayList<>();
        configuracionRepository.findAll().forEach(entity -> configuracionDTOs.add(convertToDTO(entity)));
        logger.info("Retrieved {} configuraciones from the database", configuracionDTOs.size());
        return configuracionDTOs;
    }

    @Override
    public ConfiguracionDTO findById(String clave) {
        ConfiguracionEntity configuracionEntity = configuracionRepository.findById(clave)
                .orElseThrow(() -> new EntityNotFoundException("Configuracion not found with Clave: " + clave));
        logger.info("Retrieved configuracion with Clave: {}", clave);
        return convertToDTO(configuracionEntity);
    }

    @Override
    public ConfiguracionDTO save(ConfiguracionDTO configuracionDTO) {
        ConfiguracionEntity configuracionEntity = convertToEntity(configuracionDTO);
        ConfiguracionEntity savedEntity = configuracionRepository.save(configuracionEntity);
        logger.info("Saved Configuracion with Clave: {}", savedEntity.getConfigClave());
        return convertToDTO(savedEntity);
    }

    @Override
    public ConfiguracionDTO update(String clave, ConfiguracionDTO configuracionDTO) {
        ConfiguracionEntity configuracionEntity = configuracionRepository.findById(clave)
                .orElseThrow(() -> new EntityNotFoundException("Configuracion not found with Clave: " + clave));

        // Actualizar los campos de la entidad con valores del DTO
        configuracionEntity.setConfigValor(configuracionDTO.getConfigValor());
        ConfiguracionEntity updatedEntity = configuracionRepository.save(configuracionEntity);
        logger.info("Updated Configuracion with Clave: {}", clave);
        return convertToDTO(updatedEntity);
    }

    @Override
    public boolean delete(String clave) {
        if (configuracionRepository.existsById(clave)) {
            configuracionRepository.deleteById(clave);
            logger.info("Deleted Configuracion with Clave: {}", clave);
            return true;
        }
        logger.warn("Attempted to delete Configuracion with Clave {}, but it does not exist", clave);
        throw new EntityNotFoundException("Configuracion not found with Clave: " + clave);
    }
}
