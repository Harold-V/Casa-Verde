package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CierreCaja;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Usuario;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.dto.BackupDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IBackupService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;
import com.casaverde.backend.fachadaServices.dto.CierreCajaRequest;
import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.CierreCajaMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.CierreCajaRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CierreCajaServiceImpl implements ICierreCajaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final CierreCajaRepository cierreRepo;
    private final PedidoRepository pedidoRepo;
    private final FondoRepository fondoRepo;
    private final UsuarioRepository usuarioRepo;
    private final IResumenService resumenService;
    private final IBackupService backupService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final CierreCajaMapper cierreCajaMapper;

    @Transactional(readOnly = true)
    public List<CierreCajaDTO> listar() {
        return cierreRepo.findAllByOrderByFechaDesc().stream()
                .map(cierreCajaMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CierreCajaDTO obtener(LocalDate fecha) {
        return cierreRepo.findByFecha(fecha)
                .map(cierreCajaMapper::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay cierre de caja para la fecha: " + fecha));
    }

    @Transactional
    public CierreCajaDTO cerrar(CierreCajaRequest request) {
        LocalDate fecha = request.getFecha() != null ? request.getFecha() : LocalDate.now(ZONA);
        if (cierreRepo.existsByFecha(fecha)) {
            throw new ValidacionException("La caja ya fue cerrada para la fecha: " + fecha);
        }

        long pedidosAbiertos = pedidoRepo.countByEstadoIn(EstadoPedido.activos());
        if (pedidosAbiertos > 0) {
            throw new ValidacionException("No se puede cerrar caja con " + pedidosAbiertos + " pedido(s) pendiente(s)");
        }

        Usuario usuario = null;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepo.findById(request.getUsuarioId())
                    .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado o inactivo"));
        }

        BigDecimal esperadoCop = saldoFondo("COP");
        BigDecimal esperadoUsd = saldoFondo("USD");
        BigDecimal esperadoNequi = saldoFondo("NEQUI");

        BigDecimal contadoCop = normalizarOCero("COP", request.getContadoCop(), esperadoCop);
        BigDecimal contadoUsd = normalizarOCero("USD", request.getContadoUsd(), esperadoUsd);
        BigDecimal contadoNequi = normalizarOCero("NEQUI", request.getContadoNequi(), esperadoNequi);

        ResumenDiaDTO resumen = resumenService.obtener(fecha);
        BackupDTO backup = backupService.crearBackup("cierre-" + fecha);

        CierreCaja cierre = CierreCaja.builder()
                .fecha(fecha)
                .usuarioId(usuario != null ? usuario.getId() : null)
                .usuarioNombre(usuario != null ? usuario.getNombre() : null)
                .esperadoCop(esperadoCop)
                .esperadoUsd(esperadoUsd)
                .esperadoNequi(esperadoNequi)
                .contadoCop(contadoCop)
                .contadoUsd(contadoUsd)
                .contadoNequi(contadoNequi)
                .diferenciaCop(contadoCop.subtract(esperadoCop))
                .diferenciaUsd(contadoUsd.subtract(esperadoUsd))
                .diferenciaNequi(contadoNequi.subtract(esperadoNequi))
                .cobradoTotalCop(resumen.getCobradoTotalCop())
                .gastosTotalCop(resumen.getGastosTotalCop())
                .balanceNeto(resumen.getBalanceNeto())
                .observacion(request.getObservacion())
                .backupPath(backup.getPath())
                .build();

        CierreCaja guardado = cierreRepo.save(cierre);
        auditoriaService.registrar("cierre_caja", guardado.getId(), "CERRAR",
                "Cierre de caja " + fecha + " con backup " + backup.getNombre());
        return cierreCajaMapper.toDTO(guardado);
    }

    private BigDecimal saldoFondo(String moneda) {
        return fondoRepo.findById(moneda)
                .map(Fondo::getSaldo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal normalizarOCero(String moneda, BigDecimal valor, BigDecimal fallback) {
        BigDecimal normalizado = monetaryService.normalizarSaldoFondo(moneda, valor != null ? valor : fallback);
        if (normalizado.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El conteo del fondo " + moneda + " no puede ser negativo");
        }
        return normalizado;
    }

}
