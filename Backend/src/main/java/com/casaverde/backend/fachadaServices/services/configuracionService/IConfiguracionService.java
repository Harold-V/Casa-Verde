package com.casaverde.backend.fachadaServices.services.configuracionService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.ConfiguracionDTO;

public interface IConfiguracionService {

    public List<ConfiguracionDTO> findAll();

    public ConfiguracionDTO findById(String clave);

    public ConfiguracionDTO save(ConfiguracionDTO config);

    public ConfiguracionDTO update(String clave, ConfiguracionDTO config);

    public boolean delete(String clave);

}
