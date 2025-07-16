package com.casaverde.backend.capaControladores;

import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.services.pagoService.IPagoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.PUT })
public class PagoRestController {

    @Autowired
    private IPagoService pagoService;

    @GetMapping("/pago")
    public ResponseEntity<List<PagoDTO>> listarPagos() {
        List<PagoDTO> listaPagos = pagoService.findAll();
        return new ResponseEntity<>(listaPagos, HttpStatus.OK);
    }

    @GetMapping("/pago/{id}")
    public ResponseEntity<PagoDTO> consultarPago(@PathVariable Long id) {
        PagoDTO objPago = pagoService.findById(id);
        return new ResponseEntity<>(objPago, HttpStatus.OK);
    }

    @PostMapping("/pago")
    public ResponseEntity<PagoDTO> crearPago(@RequestBody PagoDTO pago) {
        PagoDTO objPago = pagoService.save(pago);
        return new ResponseEntity<>(objPago, HttpStatus.CREATED);
    }

    @PutMapping("/pago")
    public ResponseEntity<PagoDTO> actualizarPago(@RequestBody PagoDTO pago, @RequestParam Long id) {
        PagoDTO objPago = pagoService.update(id, pago);
        return new ResponseEntity<>(objPago, HttpStatus.OK);
    }

    @DeleteMapping("/pago")
    public ResponseEntity<Boolean> eliminarPago(@RequestParam Long id) {
        Boolean bandera = pagoService.delete(id);
        return new ResponseEntity<>(bandera, HttpStatus.NO_CONTENT);
    }
}