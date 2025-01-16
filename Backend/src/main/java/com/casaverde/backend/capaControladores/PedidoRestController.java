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

import com.casaverde.backend.fachadaServices.DTO.PedidoDTO;
import com.casaverde.backend.fachadaServices.services.PedidoService.IPedidoService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.PUT })
public class PedidoRestController {

    @Autowired
    private IPedidoService pedidoService;

    // Listar todos los pedidos
    @GetMapping("/pedido")
    public ResponseEntity<List<PedidoDTO>> listarPedidos() {
        List<PedidoDTO> listaPedidos = pedidoService.findAll();
        ResponseEntity<List<PedidoDTO>> objRespuesta = new ResponseEntity<>(listaPedidos, HttpStatus.OK);
        return objRespuesta;
    }

    // Consultar un pedido por ID
    @GetMapping("/pedido/{id}")
    public ResponseEntity<PedidoDTO> consultarPedido(@PathVariable Long id) {
        PedidoDTO objPedido = pedidoService.findById(id);
        return new ResponseEntity<>(objPedido, HttpStatus.OK);
    }

    // Crear un nuevo pedido
    @PostMapping("/pedido")
    public ResponseEntity<PedidoDTO> crearPedido(@RequestBody PedidoDTO pedido) {
        PedidoDTO objPedido = pedidoService.save(pedido);
        return new ResponseEntity<>(objPedido, HttpStatus.CREATED);
    }

    // Actualizar un pedido existente
    @PutMapping("/pedido")
    public ResponseEntity<PedidoDTO> actualizarPedido(@RequestBody PedidoDTO pedido,
            @RequestParam Long id) {
        PedidoDTO objPedido = pedidoService.update(id, pedido);
        return new ResponseEntity<>(objPedido, HttpStatus.OK);
    }

    // Eliminar un pedido por ID
    @DeleteMapping("/pedido")
    public ResponseEntity<Boolean> eliminarPedido(@RequestParam Long id) {
        Boolean bandera = pedidoService.delete(id);
        return new ResponseEntity<>(bandera, HttpStatus.NO_CONTENT);
    }
}
