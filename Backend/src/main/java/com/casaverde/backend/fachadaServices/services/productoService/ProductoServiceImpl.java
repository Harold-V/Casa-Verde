package com.casaverde.backend.fachadaServices.services.productoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CategoriaProducto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoPrecioHistorial;
import com.casaverde.backend.fachadaServices.dto.ComposicionDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoPrecioHistorialDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoRequest;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.ProductoMapper;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.repositories.CategoriaProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoDetalleRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoPrecioHistorialRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements IProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaProductoRepository categoriaRepo;
    private final ProductoPrecioHistorialRepository precioHistorialRepo;
    private final PedidoDetalleRepository pedidoDetalleRepo;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final ProductoMapper productoMapper;

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarTodos() {
        return productoRepo.findAllByOrderByNombreAsc().stream()
                .map(productoMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarActivos() {
        return productoRepo.findByActivoTrueOrderByNombreAsc().stream()
                .map(productoMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoDTO buscarPorId(Long id) {
        return productoMapper.toDTO(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductoPrecioHistorialDTO> historialPrecios(Long id) {
        findOrThrow(id);
        return precioHistorialRepo.findByProductoIdOrderByRegistradoEnDesc(id).stream()
                .map(productoMapper::toPrecioHistorialDTO)
                .toList();
    }

    @Transactional
    public ProductoDTO crear(ProductoRequest req) {
        validarNombre(req.getNombre(), null);

        Producto producto = Producto.builder()
                .nombre(req.getNombre().trim())
                .precio(monetaryService.normalizarPrecioCop(req.getPrecio()))
                .afectaInventario(req.getAfectaInventario() != null ? req.getAfectaInventario() : false)
                .activo(true)
                .composicion(new ArrayList<>())
                .build();

        if (req.getCategoriaId() != null) {
            CategoriaProducto cat = categoriaRepo.findById(req.getCategoriaId())
                    .orElseThrow(
                            () -> new RecursoNoEncontradoException("Categoría no encontrada: " + req.getCategoriaId()));
            producto.setCategoria(cat);
        }

        aplicarComposicion(producto, req.getComposicion());
        Producto guardado = productoRepo.save(producto);
        registrarPrecio(guardado, null, guardado.getPrecio(), "CREAR");
        auditoriaService.registrar("producto", guardado.getId(), "CREAR",
                "Producto creado: " + guardado.getNombre());
        return productoMapper.toDTO(guardado);
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoRequest req) {
        Producto producto = findOrThrow(id);
        validarNombre(req.getNombre(), id);
        BigDecimal precioAnterior = producto.getPrecio();
        BigDecimal precioNuevo = req.getPrecio() != null
                ? monetaryService.normalizarPrecioCop(req.getPrecio())
                : precioAnterior;

        producto.setNombre(req.getNombre().trim());
        producto.setPrecio(precioNuevo);
        if (req.getAfectaInventario() != null)
            producto.setAfectaInventario(req.getAfectaInventario());
        if (req.getActivo() != null)
            producto.setActivo(req.getActivo());

        if (req.getCategoriaId() != null) {
            CategoriaProducto cat = categoriaRepo.findById(req.getCategoriaId())
                    .orElseThrow(
                            () -> new RecursoNoEncontradoException("Categoría no encontrada: " + req.getCategoriaId()));
            producto.setCategoria(cat);
        } else {
            producto.setCategoria(null);
        }

        producto.getComposicion().clear();
        aplicarComposicion(producto, req.getComposicion());

        Producto guardado = productoRepo.save(producto);
        if (precioAnterior.compareTo(precioNuevo) != 0) {
            registrarPrecio(guardado, precioAnterior, precioNuevo, "ACTUALIZAR");
            propagarPrecioAPedidosAbiertos(guardado.getId(), precioNuevo);
        }
        auditoriaService.registrar("producto", guardado.getId(), "ACTUALIZAR",
                "Producto actualizado: " + guardado.getNombre());
        return productoMapper.toDTO(guardado);
    }

    @Transactional
    public ProductoDTO actualizarPrecio(Long id, BigDecimal precio) {
        precio = monetaryService.normalizarPrecioCop(precio);
        Producto producto = findOrThrow(id);
        BigDecimal precioAnterior = producto.getPrecio();
        if (precioAnterior.compareTo(precio) == 0) {
            return productoMapper.toDTO(producto);
        }
        producto.setPrecio(precio);
        Producto guardado = productoRepo.save(producto);
        registrarPrecio(guardado, precioAnterior, precio, "ACTUALIZAR_PRECIO");
        int afectados = propagarPrecioAPedidosAbiertos(id, precio);
        auditoriaService.registrar("producto", guardado.getId(), "ACTUALIZAR_PRECIO",
                "Precio actualizado de COP " + precioAnterior + " a COP " + precio
                        + (afectados > 0 ? "; " + afectados + " detalle(s) de pedidos abiertos actualizados" : ""));
        return productoMapper.toDTO(guardado);
    }

    @Transactional
    public ProductoDTO toggleActivo(Long id) {
        Producto producto = findOrThrow(id);
        producto.setActivo(!producto.getActivo());
        Producto guardado = productoRepo.save(producto);
        auditoriaService.registrar("producto", guardado.getId(), "CAMBIAR_ESTADO",
                "Producto activo=" + guardado.getActivo());
        return productoMapper.toDTO(guardado);
    }

    /**
     * Actualiza precioUnitarioCop en todos los PedidoDetalle de pedidos abiertos
     * que referencian el producto dado. Se llama dentro de la misma transacción
     * que actualiza el Producto, garantizando consistencia.
     * Devuelve el número de detalles afectados.
     */
    private int propagarPrecioAPedidosAbiertos(Long productoId, BigDecimal nuevoPrecio) {
        List<PedidoDetalle> detalles = pedidoDetalleRepo
                .findByProductoIdAndPedidoEstadoIn(productoId, EstadoPedido.activos());
        if (detalles.isEmpty())
            return 0;
        detalles.forEach(d -> d.setPrecioUnitarioCop(nuevoPrecio));
        pedidoDetalleRepo.saveAll(detalles);
        return detalles.size();
    }

    private void validarNombre(String nombre, Long idExcluir) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidacionException("El nombre del producto es obligatorio");
        }
        boolean existe = idExcluir == null
                ? productoRepo.existsByNombre(nombre.trim())
                : productoRepo.existsByNombreAndIdNot(nombre.trim(), idExcluir);
        if (existe) {
            throw new ValidacionException("Ya existe un producto con el nombre: " + nombre.trim());
        }
    }

    private void aplicarComposicion(Producto producto, List<ComposicionDTO> items) {
        if (items == null || items.isEmpty())
            return;
        for (ComposicionDTO item : items) {
            if (item.getInsumo() == null || item.getInsumo().isBlank())
                continue;
            if (item.getCantidad() == null || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0)
                continue;
            ProductoComposicionInventario comp = ProductoComposicionInventario.builder()
                    .producto(producto)
                    .insumo(item.getInsumo().trim().toLowerCase())
                    .cantidad(item.getCantidad())
                    .build();
            producto.getComposicion().add(comp);
        }
    }

    private void registrarPrecio(Producto producto, BigDecimal anterior, BigDecimal nuevo, String motivo) {
        precioHistorialRepo.save(ProductoPrecioHistorial.builder()
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .precioAnterior(anterior)
                .precioNuevo(nuevo)
                .motivo(motivo)
                .build());
    }

    private Producto findOrThrow(Long id) {
        return productoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }

}
