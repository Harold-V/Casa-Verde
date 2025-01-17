package com.casaverde.backend.fachadaServices.services.PedidoService;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoEntity;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.fachadaServices.DTO.PedidoDTO;

@Service
public class PedidoServiceImpl implements IPedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoServiceImpl.class);

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ModelMapper modelMapper;

    private Long obtenerNuevoId(List<Long> idsExistentes) {
        if (idsExistentes.isEmpty()) {
            return 1L; // Si no hay pedidos, el ID inicial es 1
        }

        for (long i = 1L; i <= idsExistentes.size() + 1L; i++) { // Asegúrate de usar long con sufijo 'L'
            if (!idsExistentes.contains(i)) {
                return i; // Devuelve el primer ID libre
            }
        }

        return (long) idsExistentes.size() + 1L; // Asegúrate de convertir size() a Long
    }

    // Método privado para convertir de Entity a DTO
    private PedidoDTO convertToDTO(PedidoEntity pedidoEntity) {
        return modelMapper.map(pedidoEntity, PedidoDTO.class);
    }

    // Método privado para convertir de DTO a Entity
    private PedidoEntity convertToEntity(PedidoDTO pedidoDTO) {
        return modelMapper.map(pedidoDTO, PedidoEntity.class);
    }

    @Override
    public List<PedidoDTO> findAll() {
        List<PedidoDTO> pedidoDTOs = new ArrayList<>();
        pedidoRepository.findAll().forEach(entity -> pedidoDTOs.add(convertToDTO(entity)));
        logger.info("Retrieved {} pedidos from the database", pedidoDTOs.size());
        return pedidoDTOs;
    }

    @Override
    public PedidoDTO findById(Long id) {
        PedidoEntity pedidoEntity = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido not found with ID: " + id));
        logger.info("Retrieved pedido with ID: {}", id);
        return convertToDTO(pedidoEntity);
    }

    @Override
    public PedidoDTO save(PedidoDTO pedidoDTO) {
        PedidoEntity pedidoEntity = convertToEntity(pedidoDTO);

        List<Long> idsExistentes = pedidoRepository.findAllProductIds();
        Long nuevoId = obtenerNuevoId(idsExistentes);

        pedidoEntity.setPedID(nuevoId); // Asignar el menor ID libre
        PedidoEntity savedEntity = pedidoRepository.save(pedidoEntity);

        return convertToDTO(savedEntity);
    }

    @Override
    public PedidoDTO update(Long id, PedidoDTO pedidoDTO) {
        PedidoEntity pedidoEntity = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido not found with ID: " + id));

        // Actualizar los campos de la entidad con valores del DTO
        pedidoEntity.setPedNombreCliente(pedidoDTO.getPedNombreCliente());
        pedidoEntity.setPedFecha(pedidoDTO.getPedFecha());
        pedidoEntity.setPedHora(pedidoDTO.getPedHora());
        pedidoEntity.setPedValorTotal(pedidoDTO.getPedValorTotal());
        pedidoEntity.setPedEstado(pedidoDTO.getPedEstado());
        PedidoEntity updatedEntity = pedidoRepository.save(pedidoEntity);
        logger.info("Updated Pedido with ID: {}", id);
        return convertToDTO(updatedEntity);
    }

    @Override
    public boolean delete(Long id) {
        if (pedidoRepository.existsById(id)) {
            pedidoRepository.deleteById(id);
            logger.info("Deleted Pedido with ID: {}", id);
            return true;
        }
        logger.warn("Attempted to delete Pedido with ID {}, but it does not exist", id);
        throw new EntityNotFoundException("Pedido not found with ID: " + id);
    }

}
