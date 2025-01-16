package com.casaverde.backend.capaControladores;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.DTO.ConfiguracionDTO;
import com.casaverde.backend.fachadaServices.services.ConfiguracionService.IConfiguracionService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.PUT })
public class ConfiguracionRestController {

    @Autowired
    private IConfiguracionService configuracionService;

    @GetMapping("/configuracion")
    public ResponseEntity<List<ConfiguracionDTO>> listarConfiguraciones() {
        List<ConfiguracionDTO> listaConfiguraciones = configuracionService.findAll();
        return new ResponseEntity<>(listaConfiguraciones, HttpStatus.OK);
    }

    @GetMapping("/configuracion/{clave}")
    public ResponseEntity<ConfiguracionDTO> consultarConfiguracion(@PathVariable String clave) {
        ConfiguracionDTO objConfiguracion = configuracionService.findById(clave);
        return new ResponseEntity<>(objConfiguracion, HttpStatus.OK);
    }

    @PostMapping("/configuracion")
    public ResponseEntity<ConfiguracionDTO> crearConfiguracion(@RequestBody ConfiguracionDTO configuracion) {
        ConfiguracionDTO objConfiguracion = configuracionService.save(configuracion);
        return new ResponseEntity<>(objConfiguracion, HttpStatus.CREATED);
    }

    @PutMapping("/configuracion/{clave}")
    public ResponseEntity<ConfiguracionDTO> actualizarConfiguracion(@PathVariable String clave,
            @RequestBody ConfiguracionDTO configuracion) {
        ConfiguracionDTO objConfiguracion = configuracionService.update(clave, configuracion);
        return new ResponseEntity<>(objConfiguracion, HttpStatus.OK);
    }

    @DeleteMapping("/configuracion/{clave}")
    public ResponseEntity<Boolean> eliminarConfiguracion(@PathVariable String clave) {
        Boolean bandera = configuracionService.delete(clave);
        return new ResponseEntity<>(bandera, HttpStatus.NO_CONTENT);
    }
}
