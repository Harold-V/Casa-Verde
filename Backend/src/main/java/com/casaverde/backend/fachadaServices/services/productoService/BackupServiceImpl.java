package com.casaverde.backend.fachadaServices.services.productoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.casaverde.backend.fachadaServices.dto.BackupDTO;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;

@Service
public class BackupServiceImpl implements IBackupService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Value("${app.datasource.path:./DataBase/casaverde.db}")
    private String dbPath;

    @Value("${app.backup.dir:./Backups}")
    private String backupDir;

    public BackupDTO crearBackup(String motivo) {
        try {
            Path origen = Paths.get(dbPath).toAbsolutePath().normalize();
            if (!Files.exists(origen)) {
                throw new ValidacionException("No existe la base de datos para respaldar: " + origen);
            }

            Path destinoDir = Paths.get(backupDir).toAbsolutePath().normalize();
            Files.createDirectories(destinoDir);

            String timestamp = LocalDateTime.now(ZONA).format(FORMATO);
            String sufijo = limpiarMotivo(motivo);
            String nombre = "casaverde-" + timestamp + (sufijo.isBlank() ? "" : "-" + sufijo) + ".db";
            Path destino = destinoDir.resolve(nombre);

            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return toDTO(destino);
        } catch (IOException ex) {
            throw new ValidacionException("No se pudo crear el backup local: " + ex.getMessage());
        }
    }

    public List<BackupDTO> listar() {
        Path dir = Paths.get(backupDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".db"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .map(this::toDTO)
                    .toList();
        } catch (IOException ex) {
            throw new ValidacionException("No se pudo leer la carpeta de backups: " + ex.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void crearBackupAlIniciar() {
        try {
            Path origen = Paths.get(dbPath).toAbsolutePath().normalize();
            if (Files.exists(origen)) {
                crearBackup("inicio");
            }
        } catch (Exception ignored) {
            // El arranque no debe fallar por un backup; el cierre de caja si lo valida.
        }
    }

    private String limpiarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "";
        }
        return motivo.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private BackupDTO toDTO(Path path) {
        try {
            return BackupDTO.builder()
                    .nombre(path.getFileName().toString())
                    .path(path.toString())
                    .bytes(Files.size(path))
                    .creadoEn(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZONA))
                    .build();
        } catch (IOException ex) {
            return BackupDTO.builder()
                    .nombre(path.getFileName().toString())
                    .path(path.toString())
                    .bytes(0L)
                    .creadoEn(LocalDateTime.now(ZONA))
                    .build();
        }
    }

    private Instant lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException ex) {
            return Instant.EPOCH;
        }
    }
}
