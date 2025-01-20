package com.casaverde.backend.fachadaServices.services.ProductoService;

import java.util.List;
import java.util.Optional;

import com.casaverde.backend.fachadaServices.DTO.ProductoDTO;

public interface IProductoService {
    public List<ProductoDTO> findAll();

    public ProductoDTO findById(Long id);

    public ProductoDTO save(ProductoDTO producto);

    public ProductoDTO update(Long id, ProductoDTO producto);

    public boolean updateEstado(Long id);

    public Optional<Long> validarProductoParaGuardar(ProductoDTO productoDTO);

    public Optional<Long> validarProductoParaActualizar(Long id, ProductoDTO productoDTO);
}
