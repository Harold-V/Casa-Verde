package com.casaverde.backend.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ConfiguracionGeneral;
import com.casaverde.backend.capaAccesoADatos.repositories.ConfiguracionGeneralRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.TasaCambio;
import com.casaverde.backend.capaAccesoADatos.repositories.TasaCambioRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.CategoriaProducto;
import com.casaverde.backend.capaAccesoADatos.repositories.CategoriaProductoRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoComposicionRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoDetalleRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Salon;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;
import com.casaverde.backend.fachadaServices.services.productoService.IUsuarioService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoriaProductoRepository categoriaRepo;
    private final ProductoRepository productoRepo;
    private final ProductoComposicionRepository composicionRepo;
    private final SalonRepository salonRepo;
    private final TasaCambioRepository tasaRepo;
    private final ConfiguracionGeneralRepository configRepo;
    private final InventarioRepository inventarioRepo;
    private final FondoRepository fondoRepo;
    private final PedidoRepository pedidoRepo;
    private final PedidoDetalleRepository pedidoDetalleRepo;
    private final IUsuarioService usuarioService;

    @Override
    @Transactional
    public void run(String... args) {
        initCategorias();
        initProductos();
        initSalones();
        initTasaCambio();
        initConfiguracion();
        initInventario();
        initFondos();
        usuarioService.ensureUsuariosIniciales();
        backfillNequiAlias();
        backfillFinalizadoEn();
        backfillEstadosPedido();
        backfillPreciosDetalles();
        backfillAlertasInventario();
    }

    private void initCategorias() {
        if (categoriaRepo.count() > 0)
            return;
        categoriaRepo.saveAll(List.of(
                cat("Cuyes", 1),
                cat("Gallinas", 2),
                cat("Almuerzos", 3),
                cat("Complementos", 4),
                cat("Bebidas", 5)));
    }

    private void initProductos() {
        if (productoRepo.count() > 0)
            return;

        CategoriaProducto cuyes = findCat("Cuyes");
        CategoriaProducto gallinas = findCat("Gallinas");
        CategoriaProducto almuerzos = findCat("Almuerzos");
        CategoriaProducto complementos = findCat("Complementos");
        CategoriaProducto bebidas = findCat("Bebidas");

        // Cuyes
        Producto cuy = saveProducto("Cuy", 0.0, cuyes, true);
        Producto medioCuy = saveProducto("1/2 Cuy", 0.0, cuyes, true);
        Producto cuartoCuy = saveProducto("1/4 Cuy", 0.0, cuyes, true);

        // Gallinas
        Producto gallina = saveProducto("Gallina", 0.0, gallinas, true);
        Producto medioGallina = saveProducto("1/2 Gallina", 0.0, gallinas, true);
        Producto octavoGallina = saveProducto("1/8 Gallina", 0.0, gallinas, true);

        // Almuerzos
        Producto alm1 = saveProducto("Almuerzo 1/8 gallina y 1/2 cuy", 0.0, almuerzos, true);
        Producto alm2 = saveProducto("Almuerzo 1/8 gallina y 1/4 cuy", 0.0, almuerzos, true);

        // Complementos
        saveProducto("Consomé", 0.0, complementos, false);
        saveProducto("Consomé desmechado", 0.0, complementos, false);
        saveProducto("Porción Arroz", 0.0, complementos, false);
        saveProducto("Porción Papa", 0.0, complementos, false);

        // Bebidas
        saveProducto("Gaseosa", 0.0, bebidas, false);
        saveProducto("Jugo Hit", 0.0, bebidas, false);
        saveProducto("Agua", 0.0, bebidas, false);
        saveProducto("Cerveza", 0.0, bebidas, false);
        saveProducto("Pokeron", 0.0, bebidas, false);

        // Composiciones de inventario
        addComp(cuy, "cuy", new BigDecimal("1.0000"));
        addComp(medioCuy, "cuy", new BigDecimal("0.5000"));
        addComp(cuartoCuy, "cuy", new BigDecimal("0.2500"));
        addComp(gallina, "gallina", new BigDecimal("1.0000"));
        addComp(medioGallina, "gallina", new BigDecimal("0.5000"));
        addComp(octavoGallina, "gallina", new BigDecimal("0.1250"));
        addComp(alm1, "gallina", new BigDecimal("0.1250"));
        addComp(alm1, "cuy", new BigDecimal("0.5000"));
        addComp(alm2, "gallina", new BigDecimal("0.1250"));
        addComp(alm2, "cuy", new BigDecimal("0.2500"));
    }

    private void initSalones() {
        if (salonRepo.count() > 0)
            return;
        salonRepo.saveAll(List.of(
                Salon.builder().nombre("Salón principal").aforo(30).activo(true).build(),
                Salon.builder().nombre("Terraza").aforo(20).activo(true).build(),
                Salon.builder().nombre("Para llevar").activo(true).build()));
    }

    private void initTasaCambio() {
        if (tasaRepo.count() > 0)
            return;
        tasaRepo.save(TasaCambio.builder().valorCop(BigDecimal.valueOf(4200)).build());
    }

    private void initConfiguracion() {
        saveConfigIfAbsent("nombre_negocio", "Asadero Casa Verde", "Nombre del negocio");
        saveConfigIfAbsent("moneda_base", "COP", "Moneda base del sistema");
        saveConfigIfAbsent("capital_inicial_cop", "0", "Capital inicial en COP");
        saveConfigIfAbsent("mostrar_tasa_dolar", "1", "Mostrar tasa USD en topbar");
        saveConfigIfAbsent("zona_horaria", "America/Bogota", "Zona horaria del sistema");
        saveConfigIfAbsent("backup_dir", "./Backups", "Carpeta local de backups SQLite");
        saveConfigIfAbsent("limite_exento_nequi_mensual", "3404310", "Límite mensual exento de 4x1000 en NEQUI (COP)");
        saveConfigIfAbsent("cuenta_nequi_activa", "NEQUI", "Cuenta Nequi activa para pagos y movimientos");
        saveConfigIfAbsent("base_caja_cop", "0", "Base de caja inicial en COP para el conteo de denominaciones");
    }

    private void saveConfigIfAbsent(String clave, String valor, String desc) {
        if (configRepo.findById(clave).isEmpty()) {
            configRepo.save(cfg(clave, valor, desc));
        }
    }

    private void initInventario() {
        if (inventarioRepo.count() > 0)
            return;
        inventarioRepo.saveAll(List.of(
                Inventario.builder().insumo("gallina").stockActual(BigDecimal.ZERO).unidad("unidad").build(),
                Inventario.builder().insumo("cuy").stockActual(BigDecimal.ZERO).unidad("unidad").build()));
    }

    private void initFondos() {
        if (fondoRepo.count() > 0)
            return;
        fondoRepo.saveAll(List.of(
                Fondo.builder().moneda("COP").saldo(BigDecimal.ZERO).build(),
                Fondo.builder().moneda("USD").saldo(BigDecimal.ZERO).build(),
                Fondo.builder().moneda("NEQUI").alias("Principal").saldo(BigDecimal.ZERO).build()));
    }

    /**
     * Asigna alias "Principal" al fondo NEQUI existente si aún no tiene alias.
     * Idempotente — solo toca el registro si el alias es null o está en blanco.
     */
    private void backfillNequiAlias() {
        fondoRepo.findById("NEQUI").ifPresent(f -> {
            if (f.getAlias() == null || f.getAlias().isBlank()) {
                f.setAlias("Principal");
                fondoRepo.save(f);
            }
        });
    }

    /**
     * Corrige detalles de pedidos abiertos cuyo precioUnitarioCop difiere del
     * precio
     * actual del producto. Se ejecuta en cada arranque (idempotente): solo toca los
     * registros realmente desincronizados. No crea entradas en
     * ProductoPrecioHistorial
     * porque no hay cambio de precio en el producto, solo un ajuste de
     * consistencia.
     */
    private void backfillPreciosDetalles() {
        List<PedidoDetalle> desincronizados = pedidoDetalleRepo.findDesincronizados(EstadoPedido.activos());
        if (desincronizados.isEmpty())
            return;
        desincronizados.forEach(d -> d.setPrecioUnitarioCop(d.getProducto().getPrecio()));
        pedidoDetalleRepo.saveAll(desincronizados);
    }

    private void backfillFinalizadoEn() {
        List<Pedido> finalizadosSinCierre = pedidoRepo.findByEstadoOrderByCreadoEnDesc(EstadoPedido.FINALIZADO).stream()
                .filter(p -> p.getFinalizadoEn() == null)
                .peek(p -> p.setFinalizadoEn(p.getActualizadoEn() != null ? p.getActualizadoEn() : p.getCreadoEn()))
                .toList();
        if (!finalizadosSinCierre.isEmpty()) {
            pedidoRepo.saveAll(finalizadosSinCierre);
        }
    }

    private void backfillEstadosPedido() {
        List<Pedido> actualizados = pedidoRepo.findAllByOrderByCreadoEnDesc().stream()
                .filter(p -> p.getEstado() != null && p.getEstado().permiteEdicion())
                .filter(p -> {
                    BigDecimal total = p.getDetalles().stream()
                            .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal abonado = p.getPagos().stream()
                            .map(Pago::getMontoCop)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    EstadoPedido estadoActual = p.getEstado();
                    EstadoPedido estadoNuevo = EstadoPedido.calcular(total, abonado);
                    if (estadoNuevo != estadoActual) {
                        p.setEstado(estadoNuevo);
                        return true;
                    }
                    return false;
                })
                .toList();
        if (!actualizados.isEmpty()) {
            pedidoRepo.saveAll(actualizados);
        }
    }

    /**
     * Asigna alertaMinima a los insumos de inventario existentes si aún no tienen valor.
     * Idempotente — solo toca registros con alertaMinima null.
     * gallina = 5, cuy = 10.
     */
    private void backfillAlertasInventario() {
        inventarioRepo.findAll().forEach(inv -> {
            if (inv.getAlertaMinima() == null) {
                if ("gallina".equalsIgnoreCase(inv.getInsumo())) {
                    inv.setAlertaMinima(new BigDecimal("5"));
                } else if ("cuy".equalsIgnoreCase(inv.getInsumo())) {
                    inv.setAlertaMinima(new BigDecimal("10"));
                }
                inventarioRepo.save(inv);
            }
        });
    }

    // ---- helpers ----

    private CategoriaProducto findCat(String nombre) {
        return categoriaRepo.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + nombre));
    }

    private CategoriaProducto cat(String nombre, int orden) {
        return CategoriaProducto.builder().nombre(nombre).orden(orden).activo(true).build();
    }

    private Producto saveProducto(String nombre, double precio, CategoriaProducto cat, boolean afectaInventario) {
        return productoRepo.save(Producto.builder()
                .nombre(nombre).precio(BigDecimal.valueOf(precio)).categoria(cat)
                .afectaInventario(afectaInventario).activo(true).build());
    }

    private void addComp(Producto producto, String insumo, BigDecimal cantidad) {
        composicionRepo.save(ProductoComposicionInventario.builder()
                .producto(producto).insumo(insumo).cantidad(cantidad).build());
    }

    private ConfiguracionGeneral cfg(String clave, String valor, String desc) {
        return ConfiguracionGeneral.builder().clave(clave).valor(valor).descripcion(desc).build();
    }
}
