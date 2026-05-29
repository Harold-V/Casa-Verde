# Spec: Registrar Compra de Insumo (Inventario + Gasto simultáneo)

**Fecha:** 2026-05-28
**Módulo:** Inventario
**Estado:** Aprobado

---

## Contexto

El operador necesita registrar la compra de insumos (gallina, cuy) desde el módulo de inventario. Cada compra debe:
1. Sumar stock al inventario
2. Registrar el gasto asociado (cuánto costó) en el módulo de gastos

El backend ya soporta esta operación en un único endpoint: `POST /api/v1/gastos` acepta los campos opcionales `insumoInventario` + `cantidadInventario`. Cuando están presentes, el servicio registra el gasto, descuenta el fondo y genera una entrada en `MovimientoInventario` — todo en una transacción.

---

## Enfoque elegido

**Modal dedicado `ModalCompraInsumo`** en el módulo de inventario.

- No se modifica `ModalGasto` ni `ModalMovimiento`
- Una sola llamada al backend (`gastoService.registrar`)
- Formulario mínimo: 4 campos

---

## Componente: `ModalCompraInsumo`

**Archivo:** `Frontend/src/components/inventario/ModalCompraInsumo.jsx`

### Props
| Prop | Tipo | Descripción |
|---|---|---|
| `insumos` | `Array` | Stock actual — ya cargado en `InventarioPage` |
| `onClose` | `Function` | Cierra el modal |

### Campos del formulario

| Campo | Tipo | Validación | Notas |
|---|---|---|---|
| `insumo` | `select` | Requerido | Poblado desde `insumos` prop (gallina, cuy) |
| `cantidad` | `number` | Entero ≥ 1 | `step=1`, `min=1`. Muestra stock actual como referencia |
| `moneda` | `select` | Requerido | COP / USD / NEQUI — cargado desde `fondoService.listar()` |
| `monto` | `number` | > 0 | `step=100` para COP, `step=0.01` para USD/NEQUI |

### Campos NO incluidos
- Observación — omitida para agilidad operativa
- Categoría — siempre `"insumos"` (fija, no visible)
- Concepto — generado automáticamente

### Generación automática del concepto
```
"Compra ${cantidad} ${insumo}s"
// ej: "Compra 5 gallinas", "Compra 3 cuyes"
```

### Payload enviado al backend
```json
{
  "concepto": "Compra 5 gallinas",
  "categoria": "insumos",
  "moneda": "COP",
  "monto": 150000,
  "insumoInventario": "gallina",
  "cantidadInventario": 5
}
```

### Flujo al guardar
1. Validar: `cantidad >= 1` y `monto > 0`
2. Construir concepto automático
3. Llamar `gastoService.registrar(payload)`
4. Invalidar query keys: `["inventario-stock"]`, `["inventario-movimientos"]`, `["gastos"]`, `["fondos"]`
5. Cerrar modal

---

## Integración en `InventarioPage`

**Archivo:** `Frontend/src/pages/inventario/InventarioPage.jsx`

- Agregar `useState(false)` → `modalCompraAbierto`
- Agregar botón `"+ Registrar entrada"` con clase `btn btn-primary` en el `page-header`, alineado a la derecha
- Renderizar `<ModalCompraInsumo>` condicionalmente cuando `modalCompraAbierto === true`
- Pasar `insumos={stock}` y `onClose={() => setModalCompraAbierto(false)}`

### Layout del page-header resultante
```
┌──────────────────────────────────────────────┐
│  Inventario              [+ Registrar entrada]│
└──────────────────────────────────────────────┘
```

---

## Archivos modificados

| Archivo | Tipo de cambio |
|---|---|
| `Frontend/src/components/inventario/ModalCompraInsumo.jsx` | **Nuevo** |
| `Frontend/src/pages/inventario/InventarioPage.jsx` | **Modificado** — botón + modal |

## Archivos NO modificados
- `ModalGasto.jsx` — sin cambios
- `ModalMovimiento.jsx` — sin cambios
- Backend — sin cambios (endpoint ya existe)

---

## Convenciones aplicadas

- Fetching con TanStack Query (`useQuery` para fondos, invalidación con `useQueryClient`)
- Inyección de fondos igual que `ModalGasto` para monedas NEQUI dinámicas
- `useModalKeyboard({ onEscape: onClose })` para cerrar con Escape
- Clases del design system: `modal-overlay`, `modal`, `form-row`, `form-group`, `form-control`, `btn btn-primary`, `alert alert-error`
