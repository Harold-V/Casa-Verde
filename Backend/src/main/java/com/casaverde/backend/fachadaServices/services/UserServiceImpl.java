package com.casaverde.backend.fachadaServices.services;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.UserEntity;
import com.casaverde.backend.capaAccesoADatos.repositories.UserRepository;
import com.casaverde.backend.fachadaServices.DTO.UserDTO;

@Service
public class UserServiceImpl implements IUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Método privado para convertir de Entity a DTO
    private UserDTO convertToDTO(UserEntity userEntity) {
        return modelMapper.map(userEntity, UserDTO.class);
    }

    // Método privado para convertir de DTO a Entity
    private UserEntity convertToEntity(UserDTO userDTO) {
        return modelMapper.map(userDTO, UserEntity.class);
    }

    @Override
    public List<UserDTO> findAll() {
        List<UserDTO> userDTOs = new ArrayList<>();
        userRepository.findAll().forEach(entity -> userDTOs.add(convertToDTO(entity)));
        logger.info("Retrieved {} users from the database", userDTOs.size());
        return userDTOs;
    }

    @Override
    public UserDTO findById(Long id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        logger.info("Retrieved user with ID: {}", id);
        return convertToDTO(userEntity);
    }

    @Override
    public UserDTO save(UserDTO userDTO) {
        UserEntity userEntity = convertToEntity(userDTO);
        userEntity.setId(null); // Ignorar cualquier ID que venga en el DTO
        UserEntity savedEntity = userRepository.save(userEntity);
        logger.info("Saved new user with ID: {}", savedEntity.getId());
        return convertToDTO(savedEntity);
    }

    @Override
    public UserDTO update(Long id, UserDTO userDTO) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // Actualizar los campos de la entidad con valores del DTO
        userEntity.setName(userDTO.getName());
        userEntity.setEmail(userDTO.getEmail());

        UserEntity updatedEntity = userRepository.save(userEntity);
        logger.info("Updated user with ID: {}", id);
        return convertToDTO(updatedEntity);
    }

    @Override
    public boolean delete(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            logger.info("Deleted user with ID: {}", id);
            return true;
        }
        logger.warn("Attempted to delete user with ID {}, but it does not exist", id);
        throw new EntityNotFoundException("User not found with ID: " + id);
    }
}
