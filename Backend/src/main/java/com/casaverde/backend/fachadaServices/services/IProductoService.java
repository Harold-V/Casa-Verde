package com.casaverde.backend.fachadaServices.services;

import java.util.List;

import com.casaverde.backend.fachadaServices.DTO.ProductoDTO;

public interface IProductoService {
    public List<ProductoDTO> findAll();

    public ProductoDTO findById(Long id);

    public ProductoDTO save(ProductoDTO producto);

    public ProductoDTO update(Long id, ProductoDTO producto);

    public boolean delete(Long id);
}
