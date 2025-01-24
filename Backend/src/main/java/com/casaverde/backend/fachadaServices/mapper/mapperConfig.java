package com.casaverde.backend.fachadaServices.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casaverde.backend.capaAccesoADatos.models.Entitys.PagoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.PedidoAtributoOpacionalEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoEntity;
import com.casaverde.backend.capaAccesoADatos.models.Entitys.ProductoPresasEntity;
import com.casaverde.backend.capaAccesoADatos.models.Relations.PedidoProductoRelation;
import com.casaverde.backend.fachadaServices.DTO.PagoDTO;
import com.casaverde.backend.fachadaServices.DTO.PedidoAtributoOpcionalDTO;
import com.casaverde.backend.fachadaServices.DTO.PedidoProductoDTO;
import com.casaverde.backend.fachadaServices.DTO.ProductoDTO;
import com.casaverde.backend.fachadaServices.DTO.ProductoPresasDTO;

@Configuration
public class mapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Configuración para PagoEntity -> PagoDTO
        modelMapper.typeMap(PagoEntity.class, PagoDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getPedido().getPedID(), PagoDTO::setPedID);
        });

        // Configuración para ProductoPresasEntity -> PresaDTO
        modelMapper.typeMap(ProductoPresasEntity.class, ProductoPresasDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getId().getRecurso(), ProductoPresasDTO::setRecurso); // Mapea la clave recurso
            mapper.map(ProductoPresasEntity::getCantidad, ProductoPresasDTO::setCantidad); // Mapea cantidad
        });

        // Configuración para ProductoEntity -> ProductoDTO
        modelMapper.typeMap(ProductoEntity.class, ProductoDTO.class).addMappings(mapper -> {
            mapper.map(ProductoEntity::getProdID, ProductoDTO::setProdID);
            mapper.map(ProductoEntity::getProdNombre, ProductoDTO::setProdNombre);
            mapper.map(ProductoEntity::getProdPrecio, ProductoDTO::setProdPrecio);
            mapper.map(ProductoEntity::getProdEstado, ProductoDTO::setProdEstado);
            mapper.map(ProductoEntity::getPresas, ProductoDTO::setPresas); // Mapea la lista de presas
        });

        // Configuración para PedidoProductoRelation -> PedidoProductoDTO
        modelMapper.typeMap(PedidoProductoRelation.class, PedidoProductoDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getProducto().getProdID(), PedidoProductoDTO::setProdID);
            mapper.map(src -> src.getProducto().getProdNombre(), PedidoProductoDTO::setProdNombre);
            mapper.map(src -> src.getProducto().getProdPrecio(), PedidoProductoDTO::setProdPrecio);
            mapper.map(PedidoProductoRelation::getPedProdCantidad, PedidoProductoDTO::setPedProdCantidad);
        });

        // Configuración para PedidoAtributoOpcionalEntity -> PedidoAtributoOpcionalDTO
        modelMapper.typeMap(PedidoAtributoOpacionalEntity.class, PedidoAtributoOpcionalDTO.class)
                .addMappings(mapper -> {
                    mapper.map(src -> src.getId().getPedAtribOpcClave(), PedidoAtributoOpcionalDTO::setPedAtrOpcClave);
                    mapper.map(PedidoAtributoOpacionalEntity::getPedAtribOpcValor,
                            PedidoAtributoOpcionalDTO::setPedAtrOpcValor);
                });

        // Configuración para ProductoEntity -> ProductoDTO (incluye presas)
        modelMapper.typeMap(ProductoEntity.class, ProductoDTO.class).addMappings(mapper -> {
            mapper.map(ProductoEntity::getProdID, ProductoDTO::setProdID);
            mapper.map(ProductoEntity::getProdNombre, ProductoDTO::setProdNombre);
            mapper.map(ProductoEntity::getProdPrecio, ProductoDTO::setProdPrecio);
            mapper.map(ProductoEntity::getProdEstado, ProductoDTO::setProdEstado);
            mapper.map(ProductoEntity::getPresas, ProductoDTO::setPresas); // Mapear lista presas
        });

        return modelMapper;
    }
}
