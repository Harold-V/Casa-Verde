package com.casaverde.backend.fachadaServices.services.ProductoAdicionalService;
import java.util.List;
import com.casaverde.backend.fachadaServices.DTO.ProductoAdicionalDTO;

public interface IProductoAdicionalService {
    public List<ProductoAdicionalDTO> findAll();

    public ProductoAdicionalDTO findById(Long id);

    public ProductoAdicionalDTO save(ProductoAdicionalDTO producto);

    public ProductoAdicionalDTO update(Long id, ProductoAdicionalDTO producto);

    public boolean delete(Long id);
}
