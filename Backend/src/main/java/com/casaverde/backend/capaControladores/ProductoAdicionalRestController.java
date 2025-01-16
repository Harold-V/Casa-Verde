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

import com.casaverde.backend.fachadaServices.DTO.ProductoAdicionalDTO;
import com.casaverde.backend.fachadaServices.services.ProductoAdicionalService.IProductoAdicionalService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.PUT })
public class ProductoAdicionalRestController {

    @Autowired
    private IProductoAdicionalService productoAdicionalService;

    @GetMapping("/productoAdicional")
    public ResponseEntity<List<ProductoAdicionalDTO>> listarProductosAdicionales() {
        List<ProductoAdicionalDTO> listaProductosAdicionales = productoAdicionalService.findAll();
        ResponseEntity<List<ProductoAdicionalDTO>> objRespuesta = new ResponseEntity<>(listaProductosAdicionales,
                HttpStatus.OK);
        return objRespuesta;
    }

    @GetMapping("/productoAdicional/{id}")
    public ResponseEntity<ProductoAdicionalDTO> consultarProductoAdicional(@PathVariable Long id) {
        ProductoAdicionalDTO objProductoAdicional = productoAdicionalService.findById(id);
        ResponseEntity<ProductoAdicionalDTO> objRespuesta = new ResponseEntity<>(objProductoAdicional, HttpStatus.OK);
        return objRespuesta;
    }

    @PostMapping("/productoAdicional")
    public ResponseEntity<ProductoAdicionalDTO> crearProductoAdicional(@RequestBody ProductoAdicionalDTO productoAdicional) {
        ProductoAdicionalDTO objProductoAdicional = productoAdicionalService.save(productoAdicional);
        ResponseEntity<ProductoAdicionalDTO> objRespuesta = new ResponseEntity<>(objProductoAdicional, HttpStatus.CREATED);
        return objRespuesta;
    }

    @PutMapping("/productoAdicional")
    public ResponseEntity<ProductoAdicionalDTO> actualizarProductoAdicional(@RequestBody ProductoAdicionalDTO productoAdicional,
            @RequestParam Long id) {
        ProductoAdicionalDTO objProductoAdicional = productoAdicionalService.update(id, productoAdicional);
        ResponseEntity<ProductoAdicionalDTO> objRespuesta = new ResponseEntity<>(objProductoAdicional, HttpStatus.OK);
        return objRespuesta;
    }

    @DeleteMapping("/productoAdicional")
    public ResponseEntity<Boolean> eliminarProductoAdicional(@RequestParam Long id) {
        Boolean bandera = productoAdicionalService.delete(id);
        ResponseEntity<Boolean> objRespuesta = new ResponseEntity<>(bandera, HttpStatus.NO_CONTENT);
        return objRespuesta;
    }
}