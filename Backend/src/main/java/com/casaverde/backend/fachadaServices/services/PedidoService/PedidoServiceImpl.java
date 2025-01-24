package com.casaverde.backend.fachadaServices.services.PedidoService;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.ConfiguracionEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoAtributoOpacionalEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoPresasEntity;
import com.casaverde.backend.capaAccesoADatos.models.Enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.models.Relations.PedidoProductoRelation;
import com.casaverde.backend.capaAccesoADatos.repositories.ConfiguracionRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.fachadaServices.DTO.PedidoAtributoOpcionalDTO;
import com.casaverde.backend.fachadaServices.DTO.PedidoDTO;
import com.casaverde.backend.fachadaServices.DTO.PedidoProductoDTO;

@Service
public class PedidoServiceImpl implements IPedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoServiceImpl.class);

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    @Autowired
    private ProductoRepository productoRepository;

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
    public boolean updateEstado(Long id) {
        // Verificar si el producto existe
        PedidoEntity pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido not found with ID: " + id));

        // Actualizar el estado del producto a "Inactivo"
        pedido.setPedEstado(EstadoPedido.Cancelado);
        pedidoRepository.save(pedido);
        logger.info("Updated Pedido with ID: {} to Cancelado", id);

        return true;
    }

    @Transactional
    @Override
    public PedidoDTO save(PedidoDTO pedidoDTO) {
        PedidoEntity pedidoEntity = convertToEntity(pedidoDTO);
        Long nuevoId = obtenerNuevoId(pedidoRepository.findAllPedidoIds());
        pedidoEntity.setPedID(nuevoId);

        List<PedidoProductoRelation> productos = crearProductos(pedidoDTO.getProductos(), pedidoEntity);
        double valorTotal = procesarProductosYReducirStock(productos);

        pedidoEntity.setProductos(productos);
        pedidoEntity.setPedValorTotal(valorTotal);

        List<PedidoAtributoOpacionalEntity> atributosOpcionales = crearAtributosOpcionales(
                pedidoDTO.getAtributosOpcionales(), pedidoEntity);
        pedidoEntity.setAtributosOpcionales(atributosOpcionales);

        PedidoEntity savedEntity = pedidoRepository.save(pedidoEntity);
        return convertToDTO(savedEntity);
    }

    @Transactional
    @Override
    public PedidoDTO update(Long id, PedidoDTO pedidoDTO) {
        PedidoEntity pedidoExistente = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + id));

        // Actualizar los campos del pedido
        pedidoExistente.setPedNombreCliente(pedidoDTO.getPedNombreCliente());
        pedidoExistente.setPedFecha(pedidoDTO.getPedFecha());
        pedidoExistente.setPedHora(pedidoDTO.getPedHora());
        pedidoExistente.setPedEstado(pedidoDTO.getPedEstado());

        // Actualizar relaciones y manejar stock
        actualizarRelacionesAtributosOpcionales(pedidoExistente, pedidoDTO.getAtributosOpcionales());
        actualizarRelacionesProductos(pedidoExistente, pedidoDTO.getProductos());

        // Calcular el valor total sin modificar el stock
        double nuevoValorTotal = procesarProductosYCalcularValor(pedidoExistente.getProductos());
        pedidoExistente.setPedValorTotal(nuevoValorTotal);

        PedidoEntity pedidoActualizado = pedidoRepository.save(pedidoExistente);
        return convertToDTO(pedidoActualizado);
    }

    private List<PedidoAtributoOpacionalEntity> crearAtributosOpcionales(
            List<PedidoAtributoOpcionalDTO> atributosOpcionalesDTOs, PedidoEntity pedidoEntity) {

        List<PedidoAtributoOpacionalEntity> atributosOpcionales = new ArrayList<>();

        if (atributosOpcionalesDTOs != null && !atributosOpcionalesDTOs.isEmpty()) {
            for (PedidoAtributoOpcionalDTO atributoOpcionalDTO : atributosOpcionalesDTOs) {
                PedidoAtributoOpacionalEntity.PedidoAtributoOpcionalKey key = new PedidoAtributoOpacionalEntity.PedidoAtributoOpcionalKey(
                        pedidoEntity.getPedID(), atributoOpcionalDTO.getPedAtrOpcClave());
                PedidoAtributoOpacionalEntity atributoOpcional = new PedidoAtributoOpacionalEntity();
                atributoOpcional.setId(key);
                atributoOpcional.setPedAtribOpcValor(atributoOpcionalDTO.getPedAtrOpcValor());
                atributoOpcional.setPedido(pedidoEntity);
                atributosOpcionales.add(atributoOpcional);
            }
        }

        return atributosOpcionales;

    }

    private List<PedidoProductoRelation> crearProductos(List<PedidoProductoDTO> productosDTOs,
            PedidoEntity pedidoEntity) {

        List<PedidoProductoRelation> productos = new ArrayList<>();

        if (productosDTOs != null && !productosDTOs.isEmpty()) {
            for (PedidoProductoDTO productoDTO : productosDTOs) {
                PedidoProductoRelation.PedidoProductoKey key = new PedidoProductoRelation.PedidoProductoKey(
                        pedidoEntity.getPedID(), productoDTO.getProdID());

                PedidoProductoRelation producto = new PedidoProductoRelation();
                producto.setId(key);
                producto.setPedProdCantidad(productoDTO.getPedProdCantidad());
                producto.setPedido(pedidoEntity);

                // Crear y asignar un ProductoEntity simulado si es necesario
                ProductoEntity productoEntity = new ProductoEntity();
                productoEntity.setProdID(productoDTO.getProdID());
                producto.setProducto(productoEntity);

                productos.add(producto);
            }
        }

        return productos;
    }

    private void actualizarRelacionesProductos(PedidoEntity pedidoExistente, List<PedidoProductoDTO> productosDTOs) {
        // Paso 1: Restaurar el stock de todos los productos asociados al pedido actual
        for (PedidoProductoRelation relacion : pedidoExistente.getProductos()) {
            ProductoEntity producto = productoRepository.findById(relacion.getProducto().getProdID())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado con ID: " + relacion.getProducto().getProdID()));

            List<ProductoPresasEntity> presas = producto.getPresas();
            if (presas != null && !presas.isEmpty()) {
                actualizarStockPorPresas(presas, relacion.getPedProdCantidad()); // Restaurar al stock inicial
            }
        }

        // Paso 2: Identificar y eliminar relaciones obsoletas
        List<Long> nuevosIDs = productosDTOs.stream()
                .map(PedidoProductoDTO::getProdID)
                .toList();

        List<PedidoProductoRelation> relacionesObsoletas = pedidoExistente.getProductos().stream()
                .filter(rel -> !nuevosIDs.contains(rel.getProducto().getProdID()))
                .toList();

        pedidoExistente.getProductos().removeAll(relacionesObsoletas);
        pedidoRepository.eliminarRelacionesObsoletas(pedidoExistente.getPedID(), nuevosIDs);

        // Paso 3: Procesar los productos nuevos o existentes
        for (PedidoProductoDTO productoDTO : productosDTOs) {
            PedidoProductoRelation relacionExistente = pedidoExistente.getProductos().stream()
                    .filter(rel -> rel.getProducto().getProdID().equals(productoDTO.getProdID()))
                    .findFirst()
                    .orElse(null);

            if (relacionExistente != null) {
                // Si la relación ya existe, actualiza la cantidad directamente
                int nuevaCantidad = productoDTO.getPedProdCantidad();

                ProductoEntity producto = productoRepository.findById(relacionExistente.getProducto().getProdID())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Producto no encontrado con ID: " + relacionExistente.getProducto().getProdID()));

                List<ProductoPresasEntity> presas = producto.getPresas();
                if (presas != null && !presas.isEmpty()) {
                    reducirStockPorPresas(presas, nuevaCantidad); // Reducir stock según la nueva cantidad
                }

                relacionExistente.setPedProdCantidad(nuevaCantidad);
            } else {
                // Si la relación no existe, crea una nueva y ajusta el stock
                PedidoProductoRelation nuevaRelacion = new PedidoProductoRelation();
                PedidoProductoRelation.PedidoProductoKey key = new PedidoProductoRelation.PedidoProductoKey(
                        pedidoExistente.getPedID(), productoDTO.getProdID());
                nuevaRelacion.setId(key);
                nuevaRelacion.setPedProdCantidad(productoDTO.getPedProdCantidad());

                ProductoEntity productoEntity = productoRepository.findById(productoDTO.getProdID())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Producto no encontrado con ID: " + productoDTO.getProdID()));

                nuevaRelacion.setProducto(productoEntity);
                nuevaRelacion.setPedido(pedidoExistente);
                pedidoExistente.getProductos().add(nuevaRelacion);

                List<ProductoPresasEntity> presas = productoEntity.getPresas();
                if (presas != null && !presas.isEmpty()) {
                    reducirStockPorPresas(presas, productoDTO.getPedProdCantidad()); // Reducir stock por la nueva
                                                                                     // relación
                }
            }
        }
    }

    private void actualizarRelacionesAtributosOpcionales(PedidoEntity pedidoExistente,
            List<PedidoAtributoOpcionalDTO> atributosOpcionalesDTOs) {
        List<PedidoAtributoOpacionalEntity> relacionesObsoletas = new ArrayList<>();
        List<String> nuevasClaves = atributosOpcionalesDTOs.stream()
                .map(PedidoAtributoOpcionalDTO::getPedAtrOpcClave)
                .toList();

        for (PedidoAtributoOpacionalEntity relacion : pedidoExistente.getAtributosOpcionales()) {
            if (!nuevasClaves.contains(relacion.getId().getPedAtribOpcClave())) {
                relacionesObsoletas.add(relacion);
            }
        }

        pedidoExistente.getAtributosOpcionales().removeAll(relacionesObsoletas);

        for (PedidoAtributoOpcionalDTO atributoDTO : atributosOpcionalesDTOs) {
            PedidoAtributoOpacionalEntity relacionExistente = pedidoExistente.getAtributosOpcionales().stream()
                    .filter(attr -> attr.getId().getPedAtribOpcClave().equals(atributoDTO.getPedAtrOpcClave()))
                    .findFirst().orElse(null);

            if (relacionExistente != null) {
                relacionExistente.setPedAtribOpcValor(atributoDTO.getPedAtrOpcValor());
            } else {
                PedidoAtributoOpacionalEntity nuevaRelacion = new PedidoAtributoOpacionalEntity();
                PedidoAtributoOpacionalEntity.PedidoAtributoOpcionalKey key = new PedidoAtributoOpacionalEntity.PedidoAtributoOpcionalKey(
                        pedidoExistente.getPedID(), atributoDTO.getPedAtrOpcClave());
                nuevaRelacion.setId(key);
                nuevaRelacion.setPedAtribOpcValor(atributoDTO.getPedAtrOpcValor());
                nuevaRelacion.setPedido(pedidoExistente);
                pedidoExistente.getAtributosOpcionales().add(nuevaRelacion);
            }
        }
    }

    private double procesarProductosYReducirStock(List<PedidoProductoRelation> productos) {
        double valorTotal = 0.0;

        for (PedidoProductoRelation productoRelation : productos) {
            ProductoEntity producto = productoRepository.findById(productoRelation.getProducto().getProdID())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto not found with ID: " + productoRelation.getProducto().getProdID()));

            valorTotal += producto.getProdPrecio() * productoRelation.getPedProdCantidad();

            List<ProductoPresasEntity> presas = producto.getPresas();
            if (presas != null && !presas.isEmpty()) {
                reducirStockPorPresas(presas, productoRelation.getPedProdCantidad());
            }
        }

        return valorTotal;
    }

    private double procesarProductosYCalcularValor(List<PedidoProductoRelation> productos) {
        double valorTotal = 0.0;

        for (PedidoProductoRelation productoRelation : productos) {
            ProductoEntity producto = productoRepository.findById(productoRelation.getProducto().getProdID())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado con ID: " + productoRelation.getProducto().getProdID()));

            valorTotal += producto.getProdPrecio() * productoRelation.getPedProdCantidad();
        }

        return valorTotal;
    }

    private void reducirStockPorPresas(List<ProductoPresasEntity> presas, int cantidadPedido) {
        for (ProductoPresasEntity presa : presas) {
            String clave = presa.getId().getRecurso();
            int cantidadAReducir = presa.getCantidad() * cantidadPedido;
            actualizarStockConfiguracion(clave, -cantidadAReducir);
        }
    }

    private void actualizarStockPorPresas(List<ProductoPresasEntity> presas, int cantidadPedido) {
        for (ProductoPresasEntity presa : presas) {
            String clave = presa.getId().getRecurso();
            int cantidadADevolver = presa.getCantidad() * cantidadPedido;
            actualizarStockConfiguracion(clave, cantidadADevolver);
        }
    }

    private void actualizarStockConfiguracion(String clave, int cantidadAjuste) {
        ConfiguracionEntity configuracion = configuracionRepository.findById(clave)
                .orElseThrow(() -> new EntityNotFoundException("Stock no encontrado para clave: " + clave));

        int nuevoValor = Integer.parseInt(configuracion.getConfigValor()) + cantidadAjuste;
        if (nuevoValor < 0) {
            throw new IllegalArgumentException("Stock insuficiente para " + clave);
        }

        configuracion.setConfigValor(String.valueOf(nuevoValor));
        configuracionRepository.save(configuracion);
    }

}
