package com.casaverde.backend.fachadaServices.services.PagoService;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PagoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoPedido;

import com.casaverde.backend.capaAccesoADatos.repositories.PagoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.fachadaServices.DTO.PagoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoServiceImpl implements IPagoService {



    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ModelMapper modelMapper;

    private PagoDTO convertToDTO(PagoEntity pagoEntity) {
        return modelMapper.map(pagoEntity, PagoDTO.class);
    }

    private PagoEntity convertToEntity(PagoDTO pagoDTO) {
        PagoEntity pagoEntity = modelMapper.map(pagoDTO, PagoEntity.class);
        PedidoEntity pedidoEntity = pedidoRepository.findById(pagoDTO.getPedID())
                .orElseThrow(() -> new EntityNotFoundException("Pedido not found with ID: " + pagoDTO.getPedID()));
        pagoEntity.setPedido(pedidoEntity);
        return pagoEntity;
    }

    @Override
    public List<PagoDTO> findAll() {
        List<PagoEntity> pagos = (List<PagoEntity>) pagoRepository.findAll();
        return pagos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public PagoDTO findById(Long id) {
        PagoEntity pagoEntity = pagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago not found with ID: " + id));
        return convertToDTO(pagoEntity);
    }

    @Override
    @Transactional
    public PagoDTO save(PagoDTO pagoDTO) {
        PagoEntity pagoEntity = convertToEntity(pagoDTO);
        PedidoEntity pedidoEntity = pagoEntity.getPedido();

        double totalPagado = pedidoEntity.getPagos().stream().mapToDouble(PagoEntity::getPagoValor).sum() + pagoEntity.getPagoValor();
        if (totalPagado >= pedidoEntity.getPedValorTotal()) {
            pedidoEntity.setPedEstado(EstadoPedido.Finalizado);
        } else {
            pedidoEntity.setPedEstado(EstadoPedido.Pendiente);
        }

        PagoEntity savedEntity = pagoRepository.save(pagoEntity);
        return convertToDTO(savedEntity);
    }



    @Override
    public PagoDTO update(Long id, PagoDTO pagoDTO) {
        PagoEntity pagoEntity = pagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago not found with ID: " + id));

        pagoEntity.setPagoTipo(pagoDTO.getPagoTipo());
        pagoEntity.setPagoValor(pagoDTO.getPagoValor());

        PagoEntity updatedEntity = pagoRepository.save(pagoEntity);
        return convertToDTO(updatedEntity);
    }

    @Override
    public boolean delete(Long id) {
        if (pagoRepository.existsById(id)) {
            pagoRepository.deleteById(id);
            return true;
        }
        throw new EntityNotFoundException("Pago not found with ID: " + id);
    }
}