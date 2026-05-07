package com.casaverde.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.CategoriaProducto;
import com.casaverde.backend.domain.Producto;
import com.casaverde.backend.domain.ProductoComposicionInventario;
import com.casaverde.backend.dto.ComposicionDTO;
import com.casaverde.backend.dto.ProductoDTO;
import com.casaverde.backend.dto.ProductoRequest;
import com.casaverde.backend.exception.RecursoNoEncontradoException;
import com.casaverde.backend.exception.ValidacionException;
import com.casaverde.backend.repository.CategoriaProductoRepository;
import com.casaverde.backend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaProductoRepository categoriaRepo;

    public List<ProductoDTO> listarTodos() {
        return productoRepo.findAllByOrderByNombreAsc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<ProductoDTO> listarActivos() {
        return productoRepo.findByActivoTrueOrderByNombreAsc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public ProductoDTO buscarPorId(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional
    public ProductoDTO crear(ProductoRequest req) {
        validarNombre(req.getNombre(), null);

        Producto producto = Producto.builder()
                .nombre(req.getNombre().trim())
                .precio(req.getPrecio() != null ? req.getPrecio() : 0.0)
                .afectaInventario(req.getAfectaInventario() != null ? req.getAfectaInventario() : false)
                .activo(true)
                .composicion(new ArrayList<>())
                .build();

        if (req.getCategoriaId() != null) {
            CategoriaProducto cat = categoriaRepo.findById(req.getCategoriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + req.getCategoriaId()));
            producto.setCategoria(cat);
        }

        aplicarComposicion(producto, req.getComposicion());
        return toDTO(productoRepo.save(producto));
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoRequest req) {
        Producto producto = findOrThrow(id);
        validarNombre(req.getNombre(), id);

        producto.setNombre(req.getNombre().trim());
        if (req.getPrecio() != null) producto.setPrecio(req.getPrecio());
        if (req.getAfectaInventario() != null) producto.setAfectaInventario(req.getAfectaInventario());
        if (req.getActivo() != null) producto.setActivo(req.getActivo());

        if (req.getCategoriaId() != null) {
            CategoriaProducto cat = categoriaRepo.findById(req.getCategoriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + req.getCategoriaId()));
            producto.setCategoria(cat);
        } else {
            producto.setCategoria(null);
        }

        producto.getComposicion().clear();
        aplicarComposicion(producto, req.getComposicion());

        return toDTO(productoRepo.save(producto));
    }

    @Transactional
    public ProductoDTO actualizarPrecio(Long id, Double precio) {
        if (precio == null || precio < 0) {
            throw new ValidacionException("El precio debe ser mayor o igual a 0");
        }
        Producto producto = findOrThrow(id);
        producto.setPrecio(precio);
        return toDTO(productoRepo.save(producto));
    }

    @Transactional
    public ProductoDTO toggleActivo(Long id) {
        Producto producto = findOrThrow(id);
        producto.setActivo(!producto.getActivo());
        return toDTO(productoRepo.save(producto));
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
        if (items == null || items.isEmpty()) return;
        for (ComposicionDTO item : items) {
            if (item.getInsumo() == null || item.getInsumo().isBlank()) continue;
            if (item.getCantidad() == null || item.getCantidad() <= 0) continue;
            ProductoComposicionInventario comp = ProductoComposicionInventario.builder()
                    .producto(producto)
                    .insumo(item.getInsumo().trim())
                    .cantidad(item.getCantidad())
                    .build();
            producto.getComposicion().add(comp);
        }
    }

    private Producto findOrThrow(Long id) {
        return productoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }

    private ProductoDTO toDTO(Producto p) {
        List<ComposicionDTO> composicion = p.getComposicion().stream()
                .map(c -> ComposicionDTO.builder()
                        .id(c.getId())
                        .insumo(c.getInsumo())
                        .cantidad(c.getCantidad())
                        .build())
                .collect(Collectors.toList());

        return ProductoDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .precio(p.getPrecio())
                .categoriaId(p.getCategoria() != null ? p.getCategoria().getId() : null)
                .categoriaNombre(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                .afectaInventario(p.getAfectaInventario())
                .activo(p.getActivo())
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .composicion(composicion)
                .build();
    }
}
