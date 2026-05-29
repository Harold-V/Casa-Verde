# Fondos Read-Only + Ajuste a Configuración Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hacer FondosPage de solo lectura quitando el botón "Ajustar saldo" y moverlo a Configuración > General como una nueva card "Saldos de fondos".

**Architecture:** Eliminar estado/mutation/modal de ajuste de `FondosPage`. Agregar en `GeneralTab` una card con tabla de fondos + botón "Ajustar" por fila que abre el mismo `ModalAjuste` sin modificarlo. `fondos` y `invalidarFondos` ya existen en `GeneralTab` — se reutilizan.

**Tech Stack:** React 19, TanStack Query v5 (`useMutation`, `useQuery`), `ModalAjuste` (componente existente sin modificar), `fondoService.ajustar()`.

---

## Mapa de archivos

| Archivo | Acción |
|---|---|
| `Frontend/src/pages/fondos/FondosPage.jsx` | **Modificar** — eliminar todo lo relacionado con ajuste |
| `Frontend/src/pages/configuracion/tabs/GeneralTab.jsx` | **Modificar** — agregar card + mutation + ModalAjuste |
| `Frontend/src/components/fondos/ModalAjuste.jsx` | **Sin cambios** |

---

## Task 1: Quitar Ajuste de saldo de FondosPage

**Files:**
- Modify: `Frontend/src/pages/fondos/FondosPage.jsx`

- [ ] **Step 1: Eliminar import de ModalAjuste y ícono Edit2**

Reemplaza la línea de imports de lucide (línea 4-8) y el import de ModalAjuste (línea 13):

```jsx
import {
  Download, RotateCcw, TrendingUp, TrendingDown,
  DollarSign, SlidersHorizontal, Plus, ArrowLeftRight,
} from "lucide-react";
```

Y elimina completamente la línea:
```jsx
import ModalAjuste from "../../components/fondos/ModalAjuste";
```

- [ ] **Step 2: Eliminar estado `ajustando` y mutation `ajustarMut`**

Elimina estas dos líneas del cuerpo del componente:

```jsx
const [ajustando, setAjustando] = useState(null);
```

```jsx
const ajustarMut = useMutation({
  mutationFn: ({ moneda, nuevoSaldo, motivo }) => fondoService.ajustar(moneda, nuevoSaldo, motivo),
  onSuccess: invalidarFondos,
});
```

- [ ] **Step 3: Eliminar el botón de ajuste de cada tarjeta KPI**

En el bloque de tarjetas por fondo, localiza y elimina el botón completo:

```jsx
<button
  className="btn-icon"
  title="Ajustar saldo"
  onClick={() => setAjustando(f)}
>
  <Edit2 size={16} />
</button>
```

La tarjeta queda así (solo lectura):

```jsx
<div key={f.moneda} className={`card border-l-4 ${getBorderCls(f.moneda)}`}>
  <div className="p-[18px]">
    <div className="text-[13px] text-gray-500 mb-1">{getFondoLabel(f)}</div>
    <div className={`text-[28px] font-extrabold ${(f.saldo ?? 0) < 0 ? "text-red-600" : getColorCls(f.moneda)}`}>
      {f.moneda === "USD"
        ? formatUSD(f.saldo ?? 0)
        : formatCOP(f.saldo ?? 0)}
    </div>
    <div className="text-[11px] text-gray-500 mt-1">
      {f.moneda}
      {f.moneda === "USD" && tasaValor != null && (
        <span> · ≈ {formatCOP(Number(f.saldo ?? 0) * tasaValor)} COP</span>
      )}
      {f.actualizadoEn && ` · act. ${formatHora(f.actualizadoEn)}`}
    </div>
  </div>
</div>
```

- [ ] **Step 4: Eliminar el render condicional del ModalAjuste**

Localiza y elimina el bloque completo al final del JSX:

```jsx
{ajustando && (
  <ModalAjuste
    fondo={ajustando}
    onClose={() => setAjustando(null)}
    onAjustar={(moneda, nuevoSaldo, motivo) =>
      ajustarMut.mutateAsync({ moneda, nuevoSaldo, motivo })
    }
  />
)}
```

- [ ] **Step 5: Verificar que los tests siguen pasando**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)` — sin errores.

- [ ] **Step 6: Commit**

```bash
git add Frontend/src/pages/fondos/FondosPage.jsx
git commit -m "refactor(fondos): quitar ajuste de saldo — página pasa a lectura parcial"
```

---

## Task 2: Agregar card "Saldos de fondos" en GeneralTab

**Files:**
- Modify: `Frontend/src/pages/configuracion/tabs/GeneralTab.jsx`

- [ ] **Step 7: Agregar imports faltantes**

Reemplazar la línea de imports de utilidades (actualmente solo tiene `formatCOP` y `formatFecha`):

```jsx
import { formatCOP, formatFecha, formatUSD } from "../../../utils/format";
```

Y agregar el import de ModalAjuste después de los imports existentes:
```jsx
import ModalAjuste from "../../../components/fondos/ModalAjuste";
```

- [ ] **Step 8: Agregar estado `ajustando` y mutation `ajustarMut`**

Justo después del estado `creandoCuenta` (línea ~22), agregar:

```jsx
const [ajustando, setAjustando] = useState(null);
```

Justo después de `mutEliminarNequi`, agregar:

```jsx
const ajustarMut = useMutation({
  mutationFn: ({ moneda, nuevoSaldo, motivo }) =>
    fondoService.ajustar(moneda, nuevoSaldo, motivo),
  onSuccess: invalidarFondos,
});
```

- [ ] **Step 9: Agregar card "Saldos de fondos" antes de "Todos los parámetros"**

Insertar este bloque justo antes del comentario `{/* ── Todos los parámetros */}`:

```jsx
{/* ── Saldos de fondos ─────────────────────────────────────── */}
<div className="card mt-4">
  <div className="card-header"><span className="card-title">Saldos de fondos</span></div>
  <div className="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Fondo</th>
          <th className="text-right">Saldo actual</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {fondos.map((f) => {
          const label = f.moneda === "COP"
            ? "Pesos colombianos"
            : f.moneda === "USD"
            ? "Dólares"
            : `Nequi${f.alias ? ` — ${f.alias}` : ""}`;
          return (
            <tr key={f.moneda}>
              <td className="font-medium">{label}</td>
              <td className="text-right font-semibold">
                {f.moneda === "USD"
                  ? formatUSD(f.saldo ?? 0)
                  : formatCOP(f.saldo ?? 0)}
              </td>
              <td className="text-right">
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setAjustando(f)}
                >
                  Ajustar
                </button>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  </div>
</div>
```

- [ ] **Step 10: Agregar render del ModalAjuste al final del JSX**

Justo antes del cierre `</div>` final del return, agregar:

```jsx
{ajustando && (
  <ModalAjuste
    fondo={ajustando}
    onClose={() => setAjustando(null)}
    onAjustar={(moneda, nuevoSaldo, motivo) =>
      ajustarMut.mutateAsync({ moneda, nuevoSaldo, motivo })
    }
  />
)}
```

- [ ] **Step 11: Verificar que los tests siguen pasando**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 12: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/GeneralTab.jsx
git commit -m "feat(configuracion): agregar card Saldos de fondos con ajuste en GeneralTab"
```

---

## Task 3: Rebuild y verificación manual

- [ ] **Step 13: Rebuild del contenedor frontend**

```bash
cd C:/Users/HaroldV/Desktop/Casa-Verde && docker compose up --build -d frontend
```

Esperar hasta que ambos contenedores estén `healthy`.

- [ ] **Step 14: Verificar FondosPage — sin botón Ajustar**

1. Ir a `http://localhost` → módulo **Fondos**
2. Las tarjetas de COP, USD, NEQUI deben mostrar solo el saldo, sin botón de lápiz/editar
3. Los botones "Nueva entrada" y "Transferencia" deben seguir funcionando

- [ ] **Step 15: Verificar Configuración > General — card Saldos de fondos**

1. Ir a `http://localhost` → **Configuración** → pestaña **General**
2. Debe aparecer la card "Saldos de fondos" con filas para COP, USD y cada cuenta NEQUI
3. Hacer clic en "Ajustar" para COP → debe abrir el modal de ajuste
4. Cambiar el saldo, guardar → el saldo debe actualizarse en la card y en la página de Fondos
