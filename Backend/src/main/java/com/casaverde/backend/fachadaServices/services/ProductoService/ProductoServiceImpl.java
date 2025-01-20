package com.casaverde.backend.fachadaServices.services.ProductoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoPresasEntity;
import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoProducto;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoPresasRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.fachadaServices.DTO.ProductoDTO;
import com.casaverde.backend.fachadaServices.DTO.ProductoPresasDTO;

@Service
public class ProductoServiceImpl implements IProductoService {

    private static final Logger logger = LoggerFactory.getLogger(ProductoServiceImpl.class);

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoPresasRepository productoPresasRepository;

    @Autowired
    private ModelMapper modelMapper;

    private Long obtenerNuevoId(List<Long> idsExistentes) {
        if (idsExistentes.isEmpty()) {
            return 1L; // Si no hay productos, el ID inicial es 1
        }

        for (long i = 1L; i <= idsExistentes.size() + 1L; i++) {
            if (!idsExistentes.contains(i)) {
                return i; // Devuelve el primer ID libre
            }
        }

        return (long) idsExistentes.size() + 1L;
    }

    private ProductoDTO convertToDTO(ProductoEntity productoEntity) {
        return modelMapper.map(productoEntity, ProductoDTO.class);
    }

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
    @Transactional
    public ProductoDTO save(ProductoDTO productoDTO) {

        ProductoEntity productoEntity = convertToEntity(productoDTO);
        Long nuevoId = obtenerNuevoId(productoRepository.findAllProductIds());
        productoEntity.setProdID(nuevoId);

        List<ProductoPresasEntity> presasEntities = crearPresas(productoDTO.getPresas(), productoEntity);
        productoEntity.setPresas(presasEntities);

        ProductoEntity savedEntity = productoRepository.save(productoEntity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        // Buscar el producto existente
        ProductoEntity productoExistente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Eliminar presas antiguas directamente usando el repositorio
        productoPresasRepository.deleteAllByProductoProdID(id);

        // Crear y asignar las nuevas presas
        List<ProductoPresasEntity> nuevasPresas = crearPresas(productoDTO.getPresas(), productoExistente);
        productoExistente.getPresas().clear(); // Limpia la lista actual de presas
        productoExistente.getPresas().addAll(nuevasPresas); // Asigna las nuevas presas

        // Actualizar los campos del producto
        productoExistente.setProdNombre(productoDTO.getProdNombre());
        productoExistente.setProdPrecio(productoDTO.getProdPrecio());
        productoExistente.setProdEstado(productoDTO.getProdEstado());

        // Guardar el producto actualizado
        ProductoEntity productoActualizado = productoRepository.save(productoExistente);

        return convertToDTO(productoActualizado); // Retornar como DTO
    }

    @Override
    @Transactional
    public boolean updateEstado(Long id) {
        // Verificar si el producto existe
        ProductoEntity producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto not found with ID: " + id));

        // Actualizar el estado del producto a "Inactivo"
        producto.setProdEstado(EstadoProducto.Inactivo);
        productoRepository.save(producto);
        logger.info("Updated Producto with ID: {} to Inactivo", id);

        return true;
    }

    @Override
    public Optional<Long> validarProductoParaGuardar(ProductoDTO productoDTO) {
        Optional<Long> productoDuplicadoPorPresas = validarDuplicadoPorPresas(productoDTO, null);
        if (productoDuplicadoPorPresas.isPresent()) {
            return productoDuplicadoPorPresas;
        }
        return validarDuplicadoPorNombre(productoDTO.getProdNombre(), null);
    }

    @Override
    public Optional<Long> validarProductoParaActualizar(Long id, ProductoDTO productoDTO) {
        Optional<Long> productoDuplicadoPorPresas = validarDuplicadoPorPresas(productoDTO, id);
        if (productoDuplicadoPorPresas.isPresent()) {
            return productoDuplicadoPorPresas;
        }
        return validarDuplicadoPorNombre(productoDTO.getProdNombre(), id);
    }

    private Optional<Long> validarDuplicadoPorPresas(ProductoDTO productoDTO, Long idExcluir) {
        if (productoDTO.getPresas() == null || productoDTO.getPresas().isEmpty()) {
            return Optional.empty();
        }

        List<ProductoEntity> productosExistentes = StreamSupport
                .stream(productoRepository.findAll().spliterator(), false)
                .filter(producto -> idExcluir == null || !producto.getProdID().equals(idExcluir))
                .collect(Collectors.toList());

        for (ProductoEntity productoExistente : productosExistentes) {
            Set<String> conjuntoExistente = productoExistente.getPresas().stream()
                    .map(p -> p.getId().getRecurso() + ":" + p.getCantidad())
                    .collect(Collectors.toSet());

            Set<String> conjuntoNuevo = productoDTO.getPresas().stream()
                    .map(p -> p.getRecurso() + ":" + p.getCantidad())
                    .collect(Collectors.toSet());

            if (conjuntoExistente.equals(conjuntoNuevo)) {
                return Optional.of(productoExistente.getProdID());
            }
        }
        return Optional.empty();
    }

    private Optional<Long> validarDuplicadoPorNombre(String nombre, Long idExcluir) {
        String nombreNormalizado = normalizarNombre(nombre);

        return StreamSupport
                .stream(productoRepository.findAll().spliterator(), false)
                .filter(producto -> idExcluir == null || !producto.getProdID().equals(idExcluir)) // Excluir producto
                                                                                                  // actual
                .filter(producto -> normalizarNombre(producto.getProdNombre()).equals(nombreNormalizado))
                .map(ProductoEntity::getProdID)
                .findFirst();
    }

    private String normalizarNombre(String nombre) {
        return nombre.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private List<ProductoPresasEntity> crearPresas(List<ProductoPresasDTO> presasDTOs, ProductoEntity productoEntity) {
        List<ProductoPresasEntity> presasEntities = new ArrayList<>();

        if (presasDTOs != null && !presasDTOs.isEmpty()) {
            for (ProductoPresasDTO presaDTO : presasDTOs) {
                ProductoPresasEntity.ProductoPresasKey key = new ProductoPresasEntity.ProductoPresasKey(
                        productoEntity.getProdID(), presaDTO.getRecurso());

                ProductoPresasEntity presaEntity = new ProductoPresasEntity();
                presaEntity.setId(key);
                presaEntity.setCantidad(presaDTO.getCantidad());
                presaEntity.setProducto(productoEntity);
                presasEntities.add(presaEntity);
            }
        }

        return presasEntities;
    }
}
