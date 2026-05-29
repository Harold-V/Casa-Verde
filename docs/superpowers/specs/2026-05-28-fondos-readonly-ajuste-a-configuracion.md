# Spec: Fondos read-only — Ajuste de saldo a Configuración

**Fecha:** 2026-05-28
**Módulo:** Fondos + Configuración
**Estado:** Aprobado

---

## Contexto

El módulo de Fondos actualmente mezcla lectura (libro de caja, saldos) con acciones de configuración (ajuste de saldo) y operativas (nueva entrada, transferencia). El ajuste de saldo es una acción de configuración inicial — debe vivir en Configuración > General. Las acciones operativas (nueva entrada, transferencia) se quedan en Fondos.

---

## Cambios

### 1. FondosPage — quitar Ajuste de saldo

**Archivo:** `Frontend/src/pages/fondos/FondosPage.jsx`

**Eliminar:**
- Import de `ModalAjuste`
- Import del ícono `Edit2` (si queda sin uso tras este cambio)
- Estado `ajustando` (`useState(null)`)
- Mutation `ajustarMut` (y su `mutationFn`)
- Botón `Edit2` en cada tarjeta KPI de fondo
- Render condicional `{ajustando && <ModalAjuste ... />}`

**Conservar intactos:**
- `ModalEntrada` — su botón, estado, mutation y render
- `ModalTransferencia` — su botón, estado, mutation y render
- Toda la lógica del historial de movimientos y filtros
- KPIs de saldo (lectura)

---

### 2. GeneralTab — nueva card "Saldos de fondos"

**Archivo:** `Frontend/src/pages/configuracion/tabs/GeneralTab.jsx`

**Agregar:**
- Import de `ModalAjuste` desde `../../../components/fondos/ModalAjuste`
- Estado: `const [ajustando, setAjustando] = useState(null)`
- Mutation `ajustarMut`:
  ```js
  const ajustarMut = useMutation({
    mutationFn: ({ moneda, nuevoSaldo, motivo }) =>
      fondoService.ajustar(moneda, nuevoSaldo, motivo),
    onSuccess: invalidarFondos,
  });
  ```
  donde `invalidarFondos` ya existe en GeneralTab.

- Nueva card antes de "Todos los parámetros":
  ```
  ┌─ Saldos de fondos ─────────────────────────────────────┐
  │  Pesos colombianos (COP)    $500.000      [Ajustar]     │
  │  Dólares (USD)              $0.00         [Ajustar]     │
  │  Nequi — Cuenta 1           $0            [Ajustar]     │
  └────────────────────────────────────────────────────────┘
  ```
  - Itera `fondos` (ya cargado por `useQuery(["fondos"])` existente en GeneralTab)
  - Muestra etiqueta del fondo + saldo formateado (`formatCOP` o `formatUSD`)
  - Botón `btn btn-secondary btn-sm` con texto "Ajustar" por fila
  - Al hacer clic: `setAjustando(fondo)`
  - Render condicional al final del JSX: `{ajustando && <ModalAjuste fondo={ajustando} onClose={() => setAjustando(null)} onAjustar={...} />}`

**`ModalAjuste` no se modifica** — es el mismo componente reutilizado en un contexto diferente.

---

## Archivos modificados

| Archivo | Tipo de cambio |
|---|---|
| `Frontend/src/pages/fondos/FondosPage.jsx` | Eliminar ajuste (estado, mutation, botón, modal) |
| `Frontend/src/pages/configuracion/tabs/GeneralTab.jsx` | Agregar card + mutation + ModalAjuste |

## Archivos NO modificados
- `Frontend/src/components/fondos/ModalAjuste.jsx`
- `Frontend/src/components/fondos/ModalEntrada.jsx`
- `Frontend/src/components/fondos/ModalTransferencia.jsx`
- Backend — sin cambios

---

## Convenciones

- `fondos` ya está cargado en GeneralTab via `useQuery(["fondos"])` — no se duplica
- `invalidarFondos` ya existe en GeneralTab — se reutiliza
- `formatCOP` / `formatUSD` ya importados en GeneralTab
- Clase de botón: `btn btn-secondary btn-sm` (igual que otros botones de acción en configuración)
