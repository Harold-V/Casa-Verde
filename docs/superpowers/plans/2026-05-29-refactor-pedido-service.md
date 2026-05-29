# Refactor PedidoServiceImpl Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reducir `PedidoServiceImpl.java` de 668 a ~540 líneas extrayendo la lógica de inventario a `IInventarioService` y la generación de comprobante a un nuevo `IComprobanteService`.

**Architecture:** Se crean dos delegados especializados. `IComprobanteService` recibe un `PedidoDTO` ya mapeado y produce el texto plano — sin dependencias de persistencia. `IInventarioService` recibe la entidad `Pedido` y ejecuta el descuento transaccional de stock, usando los repos que ya tiene. `PedidoServiceImpl` inyecta ambos y delega con una sola línea cada uno.

**Tech Stack:** Spring Boot 3.4, Lombok `@RequiredArgsConstructor`, Jackson `ObjectMapper`, JPA repositories existentes.

---

## Mapa de archivos

| Archivo | Acción | Líneas antes → después |
|---|---|---|
| `IComprobanteService.java` | Crear | 0 → 8 |
| `ComprobanteServiceImpl.java` | Crear | 0 → ~60 |
| `IInventarioService.java` | Modificar | 20 → 23 |
| `InventarioServiceImpl.java` | Modificar | ~100 → ~180 |
| `PedidoServiceImpl.java` | Modificar | 668 → ~540 |

Todos en `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/`.

---

## Task 1: Crear IComprobanteService + ComprobanteServiceImpl

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/IComprobanteService.java`
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/ComprobanteServiceImpl.java`

- [ ] **Step 1: Crear la interfaz IComprobanteService**

```java
package com.casaverde.backend.fachadaServices.services.pedidoService;

import com.casaverde.backend.fachadaServices.dto.PedidoDTO;

public interface IComprobanteService {
    String generar(PedidoDTO dto);
}
```

- [ ] **Step 2: Crear ComprobanteServiceImpl**

```java
package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDetalleDTO;

@Service
public class ComprobanteServiceImpl implements IComprobanteService {

    @Override
    public String generar(PedidoDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("ASADERO CASA VERDE").append(System.lineSeparator());
        sb.append("Comprobante pedido #").append(dto.getId()).append(System.lineSeparator());
        sb.append("--------------------------------").append(System.lineSeparator());
        sb.append("Cliente: ").append(dto.getNombre()).append(System.lineSeparator());
        sb.append("Estado: ").append(dto.getEstado()).append(System.lineSeparator());
        if (dto.getSalonNombre() != null || dto.getMesa() != null) {
            sb.append("Ubicacion: ")
                    .append(dto.getSalonNombre() != null ? dto.getSalonNombre() : "")
                    .append(dto.getMesa() != null ? " / Mesa " + dto.getMesa() : "")
                    .append(System.lineSeparator());
        }
        if (dto.getFinalizadoEn() != null) {
            sb.append("Cierre: ")
                    .append(dto.getFinalizadoEn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append(System.lineSeparator());
        }
        sb.append("--------------------------------").append(System.lineSeparator());
        for (PedidoDetalleDTO detalle : dto.getDetalles()) {
            sb.append(detalle.getCantidad()).append(" x ")
                    .append(detalle.getNombreSnapshot())
                    .append("  COP ").append(detalle.getSubtotalCop())
                    .append(System.lineSeparator());
        }
        sb.append("--------------------------------").append(System.lineSeparator());
        sb.append("Total COP: ").append(dto.getTotalCop()).append(System.lineSeparator());
        sb.append("Abonado COP: ").append(dto.getTotalAbonado()).append(System.lineSeparator());
        if (dto.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Saldo pendiente COP: ").append(dto.getSaldoPendiente()).append(System.lineSeparator());
        }
        if (dto.getValorCambio().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Cambio COP: ").append(dto.getValorCambio()).append(System.lineSeparator());
        }
        if (!dto.getPagos().isEmpty()) {
            sb.append("--------------------------------").append(System.lineSeparator());
            sb.append("Pagos").append(System.lineSeparator());
            for (PagoDTO pago : dto.getPagos()) {
                sb.append(pago.getMoneda()).append(" ")
                        .append(pago.getMonto())
                        .append(" = COP ").append(pago.getMontoCop());
                if (pago.getTasaUsada() != null) {
                    sb.append(" tasa ").append(pago.getTasaUsada());
                }
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
```

- [ ] **Step 3: Compilar para verificar que no hay errores de sintaxis**

```bash
cd Backend
.\mvnw.cmd compile -q
```

Expected: sin output (compilación exitosa).

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/IComprobanteService.java
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/ComprobanteServiceImpl.java
git commit -m "feat(comprobante): extraer IComprobanteService desde PedidoServiceImpl"
```

---

## Task 2: Extender IInventarioService + InventarioServiceImpl

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IInventarioService.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java`

**Contexto:** `InventarioServiceImpl` ya tiene `inventarioRepo` y `movimientoRepo`. Solo necesita `ObjectMapper` (Spring Boot lo auto-configura como bean). El nuevo método `descontarPorPedido` recibe la entidad `Pedido` —consistente con el patrón del proyecto donde los services internos se pasan entidades.

- [ ] **Step 1: Agregar descontarPorPedido a IInventarioService**

Reemplazar el contenido completo de `IInventarioService.java`:

```java
package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;

public interface IInventarioService {

    List<InventarioDTO> listarStock();

    InventarioDTO obtenerStock(String insumo);

    List<MovimientoInventarioDTO> listarMovimientos();

    List<MovimientoInventarioDTO> listarMovimientosPorInsumo(String insumo);

    MovimientoInventarioDTO registrarMovimiento(MovimientoRequest req);

    void descontarPorPedido(Pedido pedido);
}
```

- [ ] **Step 2: Agregar ObjectMapper y los nuevos métodos privados a InventarioServiceImpl**

Leer el archivo actual (`InventarioServiceImpl.java`) y aplicar estos cambios:

**2a. En la sección de imports, agregar:**
```java
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoMovimientoInventario;
```

**2b. En los campos de la clase (después de `private final InventarioMapper inventarioMapper;`), agregar:**
```java
    private final ObjectMapper objectMapper;
```

**2c. Al final de la clase, antes del último `}`, agregar estos métodos:**

```java
    @Override
    @Transactional
    public void descontarPorPedido(Pedido pedido) {
        Map<String, BigDecimal> consumos = calcularConsumos(pedido);
        validarStock(consumos);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarSalida(consumo.getKey(), consumo.getValue(), pedido.getId());
        }
    }

    private Map<String, BigDecimal> calcularConsumos(Pedido pedido) {
        Map<String, BigDecimal> consumos = new LinkedHashMap<>();
        for (PedidoDetalle detalle : pedido.getDetalles()) {
            if (detalle.getComposicionSnapshot() == null || detalle.getComposicionSnapshot().isBlank())
                continue;
            for (Map<String, Object> item : leerSnapshot(detalle)) {
                Object insumoRaw = item.get("insumo");
                Object cantidadRaw = item.get("cantidad");
                if (!(insumoRaw instanceof String insumo) || insumo.isBlank()
                        || !(cantidadRaw instanceof Number)) {
                    throw new ValidacionException(
                            "Composicion de inventario invalida en el item: " + detalle.getNombreSnapshot());
                }
                BigDecimal cantPorUnidad = new BigDecimal(cantidadRaw.toString()).setScale(4, RoundingMode.HALF_UP);
                if (cantPorUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidacionException("Cantidad de inventario invalida para " + insumo
                            + " en el item: " + detalle.getNombreSnapshot());
                }
                consumos.merge(insumo.trim(),
                        cantPorUnidad.multiply(BigDecimal.valueOf(detalle.getCantidad())),
                        BigDecimal::add);
            }
        }
        return consumos;
    }

    private List<Map<String, Object>> leerSnapshot(PedidoDetalle detalle) {
        try {
            return objectMapper.readValue(detalle.getComposicionSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ValidacionException(
                    "No se pudo leer la composicion de inventario del item: " + detalle.getNombreSnapshot());
        }
    }

    private void validarStock(Map<String, BigDecimal> consumos) {
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            Inventario inv = inventarioRepo.findByInsumo(consumo.getKey())
                    .orElseThrow(() -> new ValidacionException(
                            "No existe inventario configurado para el insumo: " + consumo.getKey()));
            if (inv.getStockActual().compareTo(consumo.getValue()) < 0) {
                throw new ValidacionException("Stock insuficiente de " + consumo.getKey()
                        + ". Disponible: " + inv.getStockActual()
                        + ", requerido: " + consumo.getValue());
            }
        }
    }

    private void registrarSalida(String insumo, BigDecimal cantidad, Long pedidoId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException(
                        "No existe inventario configurado para el insumo: " + insumo));
        if (inv.getStockActual().compareTo(cantidad) < 0) {
            throw new ValidacionException("Stock insuficiente de " + insumo
                    + ". Disponible: " + inv.getStockActual()
                    + ", requerido: " + cantidad);
        }
        BigDecimal nuevo = inv.getStockActual().subtract(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo(TipoMovimientoInventario.SALIDA.valor())
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("pedido")
                .referenciaId(pedidoId)
                .observacion("Finalización pedido #" + pedidoId)
                .build());
    }
```

- [ ] **Step 3: Compilar**

```bash
cd Backend
.\mvnw.cmd compile -q
```

Expected: sin output.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IInventarioService.java
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java
git commit -m "feat(inventario): agregar descontarPorPedido a IInventarioService"
```

---

## Task 3: Refactorizar PedidoServiceImpl

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/PedidoServiceImpl.java`

**Contexto:** Estos son los cambios exactos a aplicar sobre el archivo actual:

1. Eliminar imports que ya no se usan en `PedidoServiceImpl`
2. Eliminar campos de inyección: `inventarioRepo`, `movimientoRepo`
3. Agregar campos: `inventarioService`, `comprobanteService`
4. `comprobanteTexto()` → 3 líneas delegando a `comprobanteService`
5. `finalizarPedido()` → reemplazar `descontarInventario(pedido)` por `inventarioService.descontarPorPedido(pedido)`
6. Eliminar 5 métodos privados de inventario

- [ ] **Step 1: Eliminar imports obsoletos**

Eliminar estas líneas del bloque de imports (ya no se usan después de mover el código):

```java
import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoMovimientoInventario;
```

Agregar estos imports (para los nuevos delegates):

```java
import com.casaverde.backend.fachadaServices.services.productoService.IInventarioService;
```

`IComprobanteService` ya está en el mismo paquete, no necesita import.

Nota: `LinkedHashMap`, `ObjectMapper`, `RoundingMode` se **mantienen** — los tres siguen siendo usados en métodos que permanecen en `PedidoServiceImpl` (`registrarCobroFinalPedido`, `serializarComposicion`).

- [ ] **Step 2: Reemplazar los campos de inyección**

Cambiar:

```java
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
```

por:

```java
    private final IInventarioService inventarioService;
    private final IComprobanteService comprobanteService;
```

- [ ] **Step 3: Reemplazar el cuerpo de comprobanteTexto()**

Reemplazar el método completo (líneas 86-135 actuales):

```java
    @Transactional(readOnly = true)
    public String comprobanteTexto(Long id) {
        Pedido pedido = findOrThrow(id);
        return comprobanteService.generar(pedidoMapper.toDTO(pedido));
    }
```

- [ ] **Step 4: Reemplazar la llamada en finalizarPedido()**

En `finalizarPedido()`, cambiar:

```java
        descontarInventario(pedido);
```

por:

```java
        inventarioService.descontarPorPedido(pedido);
```

- [ ] **Step 5: Eliminar los 5 métodos privados de inventario**

Eliminar completamente estos métodos (aproximadamente líneas 498-580 del archivo original):

- `private void descontarInventario(Pedido pedido)`
- `private Map<String, BigDecimal> calcularConsumosInventario(Pedido pedido)`
- `private List<Map<String, Object>> leerComposicionSnapshot(PedidoDetalle detalle)`
- `private void validarStockDisponible(Map<String, BigDecimal> consumos)`
- `private void registrarMovimientoInventario(String insumo, TipoMovimientoInventario tipo, BigDecimal cantidad, Long pedidoId)`

- [ ] **Step 6: Compilar y verificar imports no usados**

```bash
cd Backend
.\mvnw.cmd compile -q
```

Expected: sin output. Si hay advertencias de imports no usados, eliminarlos también.

- [ ] **Step 7: Verificar el tamaño resultante**

```bash
wc -l Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/PedidoServiceImpl.java
```

Expected: entre 520 y 560 líneas.

- [ ] **Step 8: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/PedidoServiceImpl.java
git commit -m "refactor(pedido): delegar inventario y comprobante a servicios especializados

PedidoServiceImpl: 668 → ~540 líneas
- descontarInventario() → inventarioService.descontarPorPedido()
- comprobanteTexto() → comprobanteService.generar() (3 líneas)
- Elimina inyección directa de InventarioRepository y MovimientoInventarioRepository"
```

---

## Task 4: Verificar tests

**Files:** ninguno (solo verificación)

- [ ] **Step 1: Correr el test suite completo**

```bash
cd Backend
.\mvnw.cmd test -q 2>&1 | tail -10
```

Expected:
```
[INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 2: Si hay fallos, identificar la causa**

Los únicos fallos esperables:
- Spring no puede crear `InventarioServiceImpl` porque el `ObjectMapper` no está auto-configurado en el test → agregar `@SpringBootTest` o mock del bean
- Inyección circular (poco probable — los beans son independientes)

Si los tests pasan, continuar.

- [ ] **Step 3: Verificar líneas finales de cada archivo**

```bash
wc -l Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/PedidoServiceImpl.java
wc -l Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java
wc -l Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/ComprobanteServiceImpl.java
```

Expected:
```
~540  PedidoServiceImpl.java
~185  InventarioServiceImpl.java
~65   ComprobanteServiceImpl.java
```

- [ ] **Step 4: Commit final**

```bash
git add -A
git commit -m "test(refactor): verificar que tests pasan tras extracción de servicios"
```
