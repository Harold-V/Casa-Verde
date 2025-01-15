package com.casaverde.backend.fachadaServices.services;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.ProductoEntity;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.fachadaServices.DTO.ProductoDTO;

@Service
public class ProductoServiceImpl implements IProductoService {

    private static final Logger logger = LoggerFactory.getLogger(ProductoServiceImpl.class);

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Método privado para convertir de Entity a DTO
    private ProductoDTO convertToDTO(ProductoEntity productoEntity) {
        return modelMapper.map(productoEntity, ProductoDTO.class);
    }

    // Método privado para convertir de DTO a Entity
    private ProductoEntity convertToEntity(ProductoDTO productoDTO) {
        return modelMapper.map(productoDTO, ProductoEntity.class);
    }

    @Override
    public List<ProductoDTO> findAll() {
        List<ProductoDTO> productoDTOs = new ArrayList<>();
        productoRepository.findAll().forEach(entity -> productoDTOs.add(convertToDTO(entity)));
        logger.info("Retrieved {} productos from the database", productoDTOs.size());
        return productoDTOs;
    }

    @Override
    public ProductoDTO findById(Long id) {
        ProductoEntity productoEntity = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto not found with ID: " + id));
        logger.info("Retrieved Producto with ID: {}", id);
        return convertToDTO(productoEntity);
    }

    @Override
    public ProductoDTO save(ProductoDTO productoDTO) {
        ProductoEntity productoEntity = convertToEntity(productoDTO);
        productoEntity.setProdID(null); // Ignorar cualquier ID que venga en el DTO
        ProductoEntity savedEntity = productoRepository.save(productoEntity);
        logger.info("Saved new Producto with ID: {}", savedEntity.getProdID());
        return convertToDTO(savedEntity);
    }

    @Override
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        ProductoEntity productoEntity = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto not found with ID: " + id));

        // Actualizar los campos de la entidad con valores del DTO
        productoEntity.setProdNombre(productoDTO.getProdNombre());
        productoEntity.setProdPrecio(productoDTO.getProdPrecio());

        ProductoEntity updatedEntity = productoRepository.save(productoEntity);
        logger.info("Updated Producto with ID: {}", id);
        return convertToDTO(updatedEntity);
    }

    @Override
    public boolean delete(Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            logger.info("Deleted Producto with ID: {}", id);
            return true;
        }
        logger.warn("Attempted to delete Producto with ID {}, but it does not exist", id);
        throw new EntityNotFoundException("Producto not found with ID: " + id);
    }

}
