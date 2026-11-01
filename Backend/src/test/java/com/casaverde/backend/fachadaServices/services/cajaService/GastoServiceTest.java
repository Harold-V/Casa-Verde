package com.casaverde.backend.fachadaServices.services.cajaService;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Gasto;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.repositories.GastoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.GastoDTO;
import com.casaverde.backend.fachadaServices.dto.GastoRequest;
import com.casaverde.backend.fachadaServices.mapper.GastoMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock private GastoRepository gastoRepo;
    @Mock private ITasaCambioService tasaCambioService;
    @Mock private InventarioRepository inventarioRepo;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepo;
    @Mock private ICajaMovimientoService cajaMovimientoService;
    @Mock private IMonetaryService monetaryService;
    @Mock private IAuditoriaService auditoriaService;
    @Mock private GastoMapper gastoMapper;

    @InjectMocks
    private GastoServiceImpl service;

    private GastoRequest request(String categoria) {
        GastoRequest req = new GastoRequest();
        req.setConcepto("Compra carbón");
        req.setMoneda("EFECTIVO");
        req.setMonto(new BigDecimal("20000"));
        req.setCategoria(categoria);
        return req;
    }

    private void stubFlujoFeliz() {
        when(monetaryService.normalizarMoneda("EFECTIVO")).thenReturn("EFECTIVO");
        when(monetaryService.normalizarMontoEntrada(eq("EFECTIVO"), any())).thenReturn(new BigDecimal("20000"));
        when(monetaryService.convertirACop(eq("EFECTIVO"), any(), any())).thenReturn(new BigDecimal("20000"));
        when(gastoRepo.save(any(Gasto.class))).thenAnswer(inv -> {
            Gasto g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });
        when(gastoMapper.toDTO(any())).thenReturn(new GastoDTO());
    }

    @Test
    void registrar_sinCategoria_noLanzaNPE_yUsaOtros() {
        // Payload real del modal "Nueva salida": no envía categoria
        stubFlujoFeliz();

        assertDoesNotThrow(() -> service.registrar(request(null)));

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepo).save(captor.capture());
        assertEquals("otros", captor.getValue().getCategoria());
        verify(cajaMovimientoService).registrar(eq(CajaMovimientoTipo.GASTO), eq("EFECTIVO"),
                eq(new BigDecimal("-20000")), eq(new BigDecimal("-20000")),
                eq("gasto"), eq(1L), anyString(), any());
    }

    @Test
    void registrar_categoriaEnMayusculas_seNormaliza() {
        stubFlujoFeliz();

        service.registrar(request(" Servicios "));

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepo).save(captor.capture());
        assertEquals("servicios", captor.getValue().getCategoria());
    }

    @Test
    void registrar_categoriaInvalida_lanzaValidacion() {
        assertThrows(ValidacionException.class, () -> service.registrar(request("comida")));
        verifyNoInteractions(gastoRepo, cajaMovimientoService);
    }
}
