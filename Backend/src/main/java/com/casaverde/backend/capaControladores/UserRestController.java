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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.DTO.UserDTO;
import com.casaverde.backend.fachadaServices.services.IUserService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.PUT })
public class UserRestController {

    @Autowired
    private IUserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listarSolicitudes() {
        List<UserDTO> lista = userService.findAll();
        ResponseEntity<List<UserDTO>> objRespuesta = new ResponseEntity<List<UserDTO>>(lista, HttpStatus.OK);
        return objRespuesta;
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> consultarSolicitud(@PathVariable Long id) {
        UserDTO objSolicitud = null;
        objSolicitud = userService.findById(id);
        ResponseEntity<UserDTO> objRespuesta = new ResponseEntity<UserDTO>(objSolicitud, HttpStatus.OK);
        return objRespuesta;
    }

    @PostMapping("/users")
    public ResponseEntity<UserDTO> crearSolicitud(@RequestBody UserDTO solicitud) {
        UserDTO objSolicitud = null;
        objSolicitud = userService.save(solicitud);
        ResponseEntity<UserDTO> objRespuesta = new ResponseEntity<UserDTO>(objSolicitud, HttpStatus.CREATED);
        return objRespuesta;
    }

    @PutMapping("/users")
    public ResponseEntity<UserDTO> actualizarSolicitud(@RequestBody UserDTO solicitud,
            @RequestParam Long id) {
        UserDTO objSolicitud = userService.update(id, solicitud);
        ResponseEntity<UserDTO> objRespuesta = new ResponseEntity<UserDTO>(objSolicitud, HttpStatus.OK);
        return objRespuesta;
    }

    @DeleteMapping("/users")
    public ResponseEntity<Boolean> eliminarSolicitud(@RequestParam Long id) {
        Boolean bandera = userService.delete(id);
        ResponseEntity<Boolean> objRespuesta = new ResponseEntity<Boolean>(bandera, HttpStatus.NO_CONTENT);
        return objRespuesta;
    }

}
