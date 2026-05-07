package com.casaverde.backend.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.domain.*;
import com.casaverde.backend.repository.*;

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

    @Override
    @Transactional
    public void run(String... args) {
        initCategorias();
        initProductos();
        initSalones();
        initTasaCambio();
        initConfiguracion();
        initInventario();
    }

    private void initCategorias() {
        if (categoriaRepo.count() > 0) return;
        categoriaRepo.saveAll(List.of(
            cat("Cuyes", 1),
            cat("Gallinas", 2),
            cat("Almuerzos", 3),
            cat("Complementos", 4),
            cat("Bebidas", 5)
        ));
    }

    private void initProductos() {
        if (productoRepo.count() > 0) return;

        CategoriaProducto cuyes    = categoriaRepo.findByActivoTrueOrderByOrdenAsc().get(0);
        CategoriaProducto gallinas = categoriaRepo.findByActivoTrueOrderByOrdenAsc().get(1);
        CategoriaProducto almuerzos = categoriaRepo.findByActivoTrueOrderByOrdenAsc().get(2);
        CategoriaProducto complementos = categoriaRepo.findByActivoTrueOrderByOrdenAsc().get(3);
        CategoriaProducto bebidas  = categoriaRepo.findByActivoTrueOrderByOrdenAsc().get(4);

        // Cuyes
        Producto cuy       = saveProducto("Cuy",       0.0, cuyes,       true);
        Producto medioCuy  = saveProducto("1/2 Cuy",   0.0, cuyes,       true);
        Producto cuartoCuy = saveProducto("1/4 Cuy",   0.0, cuyes,       true);

        // Gallinas
        Producto gallina      = saveProducto("Gallina",       0.0, gallinas, true);
        Producto medioGallina = saveProducto("1/2 Gallina",   0.0, gallinas, true);
        Producto octavoGallina = saveProducto("1/8 Gallina",  0.0, gallinas, true);

        // Almuerzos
        Producto alm1 = saveProducto("Almuerzo 1/8 gallina y 1/2 cuy", 0.0, almuerzos, true);
        Producto alm2 = saveProducto("Almuerzo 1/8 gallina y 1/4 cuy", 0.0, almuerzos, true);

        // Complementos
        saveProducto("Consomé",         0.0, complementos, false);
        saveProducto("Consomé desmechado", 0.0, complementos, false);
        saveProducto("Porción Arroz",   0.0, complementos, false);
        saveProducto("Porción Papa",    0.0, complementos, false);

        // Bebidas
        saveProducto("Gaseosa",  0.0, bebidas, false);
        saveProducto("Jugo Hit", 0.0, bebidas, false);
        saveProducto("Agua",     0.0, bebidas, false);
        saveProducto("Cerveza",  0.0, bebidas, false);
        saveProducto("Pokeron",  0.0, bebidas, false);

        // Composiciones de inventario
        addComp(cuy,       "cuy",     1.0);
        addComp(medioCuy,  "cuy",     0.5);
        addComp(cuartoCuy, "cuy",     0.25);
        addComp(gallina,      "gallina", 1.0);
        addComp(medioGallina, "gallina", 0.5);
        addComp(octavoGallina, "gallina", 0.125);
        addComp(alm1, "gallina", 0.125);
        addComp(alm1, "cuy",     0.5);
        addComp(alm2, "gallina", 0.125);
        addComp(alm2, "cuy",     0.25);
    }

    private void initSalones() {
        if (salonRepo.count() > 0) return;
        salonRepo.saveAll(List.of(
            Salon.builder().nombre("Salón principal").aforo(30).activo(true).build(),
            Salon.builder().nombre("Terraza").aforo(20).activo(true).build(),
            Salon.builder().nombre("Para llevar").activo(true).build()
        ));
    }

    private void initTasaCambio() {
        if (tasaRepo.count() > 0) return;
        tasaRepo.save(TasaCambio.builder().valorCop(4200.0).build());
    }

    private void initConfiguracion() {
        if (configRepo.count() > 0) return;
        configRepo.saveAll(List.of(
            cfg("nombre_negocio",      "Asadero Casa Verde",  "Nombre del negocio"),
            cfg("moneda_base",         "COP",                 "Moneda base del sistema"),
            cfg("capital_inicial_cop", "0",                   "Capital inicial en COP"),
            cfg("mostrar_tasa_dolar",  "1",                   "Mostrar tasa USD en topbar")
        ));
    }

    private void initInventario() {
        if (inventarioRepo.count() > 0) return;
        inventarioRepo.saveAll(List.of(
            Inventario.builder().insumo("gallina").stockActual(0.0).unidad("unidad").build(),
            Inventario.builder().insumo("cuy").stockActual(0.0).unidad("unidad").build()
        ));
    }

    // ---- helpers ----

    private CategoriaProducto cat(String nombre, int orden) {
        return CategoriaProducto.builder().nombre(nombre).orden(orden).activo(true).build();
    }

    private Producto saveProducto(String nombre, double precio, CategoriaProducto cat, boolean afectaInventario) {
        return productoRepo.save(Producto.builder()
                .nombre(nombre).precio(precio).categoria(cat)
                .afectaInventario(afectaInventario).activo(true).build());
    }

    private void addComp(Producto producto, String insumo, double cantidad) {
        composicionRepo.save(ProductoComposicionInventario.builder()
                .producto(producto).insumo(insumo).cantidad(cantidad).build());
    }

    private ConfiguracionGeneral cfg(String clave, String valor, String desc) {
        return ConfiguracionGeneral.builder().clave(clave).valor(valor).descripcion(desc).build();
    }
}
