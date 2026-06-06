package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.enums.RolUsuario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Usuario;
import com.casaverde.backend.config.security.AuthTokenService;
import com.casaverde.backend.fachadaServices.dto.CambiarPasswordRequest;
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.LoginRequest;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.UsuarioMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final IAuditoriaService auditoriaService;
    private final UsuarioMapper usuarioMapper;
    private final AuthTokenService authTokenService;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.admin.default-password:admin123}")
    private String defaultAdminPassword;

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarActivos() {
        return usuarioRepo.findByActivoTrueOrderByNombreAsc().stream()
                .map(usuarioMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepo.findAllByOrderByNombreAsc().stream()
                .map(usuarioMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO login(LoginRequest request) {
        if (request.getUsuarioId() == null) {
            throw new ValidacionException("Seleccione un usuario");
        }
        Usuario usuario = usuarioRepo.findById(request.getUsuarioId())
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado o inactivo"));

        if (Boolean.TRUE.equals(usuario.getRequierePassword())) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new ValidacionException("La clave del administrador es obligatoria");
            }
            if (usuario.getPasswordHash() == null || !matches(request.getPassword(), usuario.getPasswordHash())) {
                throw new ValidacionException("Clave de administrador incorrecta");
            }
        }

        UsuarioDTO dto = usuarioMapper.toDTO(usuario);
        dto.setToken(authTokenService.generar(usuario));
        return dto;
    }

    @Transactional
    public void ensureUsuariosIniciales() {
        if (usuarioRepo.count() > 0) {
            return;
        }

        Usuario admin = Usuario.builder()
                .nombre("Administrador")
                .rol(RolUsuario.ADMIN.name())
                .requierePassword(true)
                .passwordHash(hashPassword(defaultAdminPassword))
                .activo(true)
                .build();
        Usuario operador = Usuario.builder()
                .nombre("Operador")
                .rol(RolUsuario.OPERADOR.name())
                .requierePassword(false)
                .activo(true)
                .build();

        usuarioRepo.saveAll(List.of(admin, operador));
        auditoriaService.registrar("usuario", null, "INICIALIZAR",
                "Usuarios locales iniciales creados: Administrador y Operador");
    }

    @Transactional
    public UsuarioDTO crearOperador(CrearOperadorRequest request) {
        if (usuarioRepo.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ValidacionException("Ya existe un usuario con ese nombre");
        }
        Usuario u = Usuario.builder()
                .nombre(request.getNombre())
                .rol(RolUsuario.OPERADOR.name())
                .requierePassword(false)
                .activo(true)
                .build();
        usuarioRepo.save(u);
        auditoriaService.registrar("usuario", u.getId(), "CREAR",
                "Operador creado: " + u.getNombre());
        return usuarioMapper.toDTO(u);
    }

    @Transactional
    public UsuarioDTO toggleActivo(Long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (Boolean.TRUE.equals(u.getActivo()) && RolUsuario.ADMIN.name().equals(u.getRol())) {
            long adminsActivos = usuarioRepo.countByRolAndActivoTrue(RolUsuario.ADMIN.name());
            if (adminsActivos <= 1) {
                throw new ValidacionException("No se puede desactivar el único administrador activo");
            }
        }
        u.setActivo(!Boolean.TRUE.equals(u.getActivo()));
        auditoriaService.registrar("usuario", id, "TOGGLE",
                "Usuario " + u.getNombre() + ": activo=" + u.getActivo());
        return usuarioMapper.toDTO(u);
    }

    @Transactional
    public UsuarioDTO cambiarPassword(Long id, CambiarPasswordRequest request) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(u.getRequierePassword())) {
            throw new ValidacionException("Este usuario no maneja contraseña");
        }
        u.setPasswordHash(hashPassword(request.getNuevaPassword()));
        auditoriaService.registrar("usuario", id, "CAMBIAR_PASSWORD",
                "Contraseña actualizada para: " + u.getNombre());
        return usuarioMapper.toDTO(u);
    }

    @Transactional
    public void eliminarOperador(Long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (RolUsuario.ADMIN.name().equals(u.getRol())) {
            throw new ValidacionException("No se puede eliminar un administrador");
        }
        auditoriaService.registrar("usuario", id, "ELIMINAR",
                "Operador eliminado: " + u.getNombre());
        usuarioRepo.delete(u);
    }

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private boolean matches(String rawPassword, String storedHash) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }

}
