package com.casaverde.backend.capaControladores;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.DTO.ProductoDTO;
import com.casaverde.backend.fachadaServices.services.ProductoService.IProductoService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.PUT })
public class ProductoRestController {

    @Autowired
    private IProductoService productoService;

    @GetMapping("/producto")
    public ResponseEntity<List<ProductoDTO>> listarProductos() {
        List<ProductoDTO> listaProductos = productoService.findAll();
        ResponseEntity<List<ProductoDTO>> objRespuesta = new ResponseEntity<List<ProductoDTO>>(listaProductos,
                HttpStatus.OK);
        return objRespuesta;
    }

    @GetMapping("/producto/{id}")
    public ResponseEntity<ProductoDTO> consultarProducto(@PathVariable Long id) {
        ProductoDTO objProducto = null;
        objProducto = productoService.findById(id);
        ResponseEntity<ProductoDTO> objRespuesta = new ResponseEntity<ProductoDTO>(objProducto, HttpStatus.OK);
        return objRespuesta;
    }

    @PostMapping("/producto")
    public ResponseEntity<ProductoDTO> crearProducto(@RequestBody ProductoDTO producto) {
        ProductoDTO objProducto = null;
        objProducto = productoService.save(producto);
        ResponseEntity<ProductoDTO> objRespuesta = new ResponseEntity<ProductoDTO>(objProducto, HttpStatus.CREATED);
        return objRespuesta;
    }

    @PutMapping("/producto")
    public ResponseEntity<ProductoDTO> actualizarProducto(@RequestBody ProductoDTO producto,
            @RequestParam Long id) {
        ProductoDTO objProducto = productoService.update(id, producto);
        ResponseEntity<ProductoDTO> objRespuesta = new ResponseEntity<ProductoDTO>(objProducto, HttpStatus.OK);
        return objRespuesta;
    }

    @PutMapping("/producto/eliminar/{id}")
    public ResponseEntity<Boolean> eliminarProducto(@PathVariable Long id) {
        Boolean bandera = productoService.updateEstado(id);
        ResponseEntity<Boolean> objRespuesta = new ResponseEntity<Boolean>(bandera, HttpStatus.NO_CONTENT);
        return objRespuesta;
    }

    @GetMapping("/producto/validarParaGuardar")
    public ResponseEntity<?> validarProductoParaGuardar(@RequestBody ProductoDTO producto) {
        Optional<Long> productoId = productoService.validarProductoParaGuardar(producto);

        if (productoId.isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe un producto con el ID: " + productoId.get());
        } else {
            return ResponseEntity.ok("El producto puede ser guardado");
        }
    }

    @GetMapping("/producto/validarParaActualizar/{id}")
    public ResponseEntity<?> validarProductoParaActualizar(@PathVariable Long id, @RequestBody ProductoDTO producto) {
        Optional<Long> productoId = productoService.validarProductoParaActualizar(id, producto);

        if (productoId.isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe un producto con el ID: " + productoId.get());
        } else {
            return ResponseEntity.ok("El producto puede ser actualizado");
        }
    }

}