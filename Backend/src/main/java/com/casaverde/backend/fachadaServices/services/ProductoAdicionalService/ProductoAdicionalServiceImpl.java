package com.casaverde.backend.fachadaServices.services.ProductoAdicionalService;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.ProductoAdicionalEntity;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoAdicionalRepository;
import com.casaverde.backend.fachadaServices.DTO.ProductoAdicionalDTO;

@Service
public class ProductoAdicionalServiceImpl implements IProductoAdicionalService {

    private static final Logger logger = LoggerFactory.getLogger(ProductoAdicionalServiceImpl.class);

    @Autowired
    private ProductoAdicionalRepository productoAdicionalRepository;

    @Autowired
    private ModelMapper modelMapper;

    private Long obtenerNuevoId(List<Long> idsExistentes) {
        if (idsExistentes.isEmpty()) {
            return 1L; // Si no hay productos adicionales, el ID inicial es 1
        }

        for (long i = 1L; i <= idsExistentes.size() + 1L; i++) { // Asegúrate de usar long con sufijo 'L'
            if (!idsExistentes.contains(i)) {
                return i; // Devuelve el primer ID libre
            }
        }

        return (long) idsExistentes.size() + 1L; // Asegúrate de convertir size() a Long
    }

    // Método privado para convertir de Entity a DTO
    private ProductoAdicionalDTO convertToDTO(ProductoAdicionalEntity productoAdicionalEntity) {
        return modelMapper.map(productoAdicionalEntity, ProductoAdicionalDTO.class);
    }

    // Método privado para convertir de DTO a Entity
    private ProductoAdicionalEntity convertToEntity(ProductoAdicionalDTO productoAdicionalDTO) {
        return modelMapper.map(productoAdicionalDTO, ProductoAdicionalEntity.class);
    }

    @Override
    public List<ProductoAdicionalDTO> findAll() {
        List<ProductoAdicionalDTO> productoAdicionalDTOs = new ArrayList<>();
        productoAdicionalRepository.findAll().forEach(entity -> productoAdicionalDTOs.add(convertToDTO(entity)));
        logger.info("Retrieved {} productos adicionales from the database", productoAdicionalDTOs.size());
        return productoAdicionalDTOs;
    }

    @Override
    public ProductoAdicionalDTO findById(Long id) {
        ProductoAdicionalEntity productoAdicionalEntity = productoAdicionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto adicional not found with ID: " + id));
        logger.info("Retrieved Producto adicional with ID: {}", id);
        return convertToDTO(productoAdicionalEntity);
    }

    @Override
    public ProductoAdicionalDTO save(ProductoAdicionalDTO productoAdicionalDTO) {
        ProductoAdicionalEntity productoAdicionalEntity = convertToEntity(productoAdicionalDTO);

        List<Long> idsExistentes = productoAdicionalRepository.findAllProductIds();
        Long nuevoId = obtenerNuevoId(idsExistentes);

        productoAdicionalEntity.setProdAdID(nuevoId); // Asignar el menor ID libre
        ProductoAdicionalEntity savedEntity = productoAdicionalRepository.save(productoAdicionalEntity);

        return convertToDTO(savedEntity);
    }

    @Override
    public ProductoAdicionalDTO update(Long id, ProductoAdicionalDTO productoAdicionalDTO) {
        ProductoAdicionalEntity productoAdicionalEntity = productoAdicionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto adicional not found with ID: " + id));

        // Actualizar los campos de la entidad con valores del DTO
        productoAdicionalEntity.setProdAdNombre(productoAdicionalDTO.getProdAdNombre());
        productoAdicionalEntity.setProdAdPrecio(productoAdicionalDTO.getProdAdPrecio());

        ProductoAdicionalEntity updatedEntity = productoAdicionalRepository.save(productoAdicionalEntity);
        logger.info("Updated Producto adicional with ID: {}", id);
        return convertToDTO(updatedEntity);
    }

    @Override
    public boolean delete(Long id) {
        if (productoAdicionalRepository.existsById(id)) {
            productoAdicionalRepository.deleteById(id);
            logger.info("Deleted Producto adicional with ID: {}", id);
            return true;
        }
        logger.warn("Attempted to delete Producto adicional with ID {}, but it does not exist", id);
        throw new EntityNotFoundException("Producto adicional not found with ID: " + id);
    }
}