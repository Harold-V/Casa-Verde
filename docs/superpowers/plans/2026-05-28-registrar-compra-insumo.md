# Registrar Compra de Insumo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar botón "Registrar entrada" en InventarioPage que abre un modal para comprar gallinas o cuyes, registrando simultáneamente el stock y el gasto en una sola llamada al backend.

**Architecture:** El backend ya soporta la operación en `POST /api/v1/gastos` mediante los campos `insumoInventario` + `cantidadInventario`. El frontend solo necesita un nuevo `ModalCompraInsumo` que construya el payload correcto. `InventarioPage` añade un botón en su header que abre este modal.

**Tech Stack:** React 19, TanStack Query v5 (`useQuery`, `useQueryClient`), `gastoService.registrar()`, `fondoService.listar()`, Vitest + Testing Library + userEvent.

---

## Mapa de archivos

| Archivo | Acción |
|---|---|
| `Frontend/src/components/inventario/ModalCompraInsumo.jsx` | **Crear** — modal con 4 campos |
| `Frontend/src/components/inventario/__tests__/ModalCompraInsumo.test.jsx` | **Crear** — tests del modal |
| `Frontend/src/pages/inventario/InventarioPage.jsx` | **Modificar** — botón + apertura del modal |

---

## Task 1: Crear test file para ModalCompraInsumo

**Files:**
- Create: `Frontend/src/components/inventario/__tests__/ModalCompraInsumo.test.jsx`

- [ ] **Step 1: Crear el archivo de test**

```jsx
// Frontend/src/components/inventario/__tests__/ModalCompraInsumo.test.jsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

vi.mock("../../../services/gastoService", () => ({
  gastoService: { registrar: vi.fn() },
}));

vi.mock("../../../services/fondoService", () => ({
  fondoService: {
    listar: vi.fn().mockResolvedValue([
      { moneda: "COP", saldo: 1000000 },
      { moneda: "USD", saldo: 200 },
    ]),
  },
}));

import ModalCompraInsumo from "../ModalCompraInsumo";
import { gastoService } from "../../../services/gastoService";

const INSUMOS = [
  { insumo: "gallina", stockActual: 10 },
  { insumo: "cuy",     stockActual: 5  },
];

function wrapper({ children }) {
  return (
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      {children}
    </QueryClientProvider>
  );
}

describe("ModalCompraInsumo", () => {
  let onClose;

  beforeEach(() => {
    onClose = vi.fn();
    vi.clearAllMocks();
  });

  // ─── Renderizado ─────────────────────────────────────────────────────────

  it("muestra el título 'Registrar entrada de insumo'", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    expect(screen.getByText("Registrar entrada de insumo")).toBeInTheDocument();
  });

  it("renderiza los campos insumo, cantidad, moneda y monto", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    expect(screen.getByLabelText(/insumo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/cantidad/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/moneda/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/monto/i)).toBeInTheDocument();
  });

  it("NO muestra campo de observación", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    expect(screen.queryByLabelText(/observaci/i)).not.toBeInTheDocument();
  });

  it("muestra las opciones de insumo (gallina y cuy)", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    const select = screen.getByLabelText(/insumo/i);
    expect(select).toHaveValue("gallina");
    const options = Array.from(select.options).map((o) => o.value);
    expect(options).toContain("gallina");
    expect(options).toContain("cuy");
  });

  // ─── Generación automática del concepto ──────────────────────────────────

  it("genera concepto 'Compra 5 gallinas' al registrar 5 gallinas", async () => {
    gastoService.registrar.mockResolvedValue({});
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.clear(screen.getByLabelText(/cantidad/i));
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.clear(screen.getByLabelText(/monto/i));
    await userEvent.type(screen.getByLabelText(/monto/i), "150000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await waitFor(() =>
      expect(gastoService.registrar).toHaveBeenCalledWith(
        expect.objectContaining({ concepto: "Compra 5 gallinas" })
      )
    );
  });

  it("genera concepto 'Compra 3 cuyes' al registrar 3 cuyes", async () => {
    gastoService.registrar.mockResolvedValue({});
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.selectOptions(screen.getByLabelText(/insumo/i), "cuy");
    await userEvent.clear(screen.getByLabelText(/cantidad/i));
    await userEvent.type(screen.getByLabelText(/cantidad/i), "3");
    await userEvent.clear(screen.getByLabelText(/monto/i));
    await userEvent.type(screen.getByLabelText(/monto/i), "90000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await waitFor(() =>
      expect(gastoService.registrar).toHaveBeenCalledWith(
        expect.objectContaining({ concepto: "Compra 3 cuyes" })
      )
    );
  });

  // ─── Payload correcto ─────────────────────────────────────────────────────

  it("envía categoria='insumos', insumoInventario e cantidadInventario correctos", async () => {
    gastoService.registrar.mockResolvedValue({});
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.clear(screen.getByLabelText(/cantidad/i));
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.clear(screen.getByLabelText(/monto/i));
    await userEvent.type(screen.getByLabelText(/monto/i), "150000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await waitFor(() =>
      expect(gastoService.registrar).toHaveBeenCalledWith({
        concepto: "Compra 5 gallinas",
        categoria: "insumos",
        moneda: "COP",
        monto: 150000,
        insumoInventario: "gallina",
        cantidadInventario: 5,
      })
    );
  });

  // ─── Validaciones ─────────────────────────────────────────────────────────

  it("muestra error si cantidad está vacía", async () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    await userEvent.type(screen.getByLabelText(/monto/i), "50000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));
    expect(screen.getByText(/cantidad debe ser al menos 1/i)).toBeInTheDocument();
  });

  it("muestra error si monto está vacío", async () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));
    expect(screen.getByText(/monto debe ser mayor a 0/i)).toBeInTheDocument();
  });

  it("muestra error del backend si el servicio falla", async () => {
    gastoService.registrar.mockRejectedValue(new Error("Saldo insuficiente en el fondo"));
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.type(screen.getByLabelText(/monto/i), "150000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await screen.findByText("Saldo insuficiente en el fondo");
  });

  // ─── Cierre ───────────────────────────────────────────────────────────────

  it("llama onClose al hacer clic en Cancelar", async () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    await userEvent.click(screen.getByRole("button", { name: /cancelar/i }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
```

- [ ] **Step 2: Ejecutar los tests — deben FALLAR (componente no existe aún)**

```bash
cd Frontend && npm run test -- ModalCompraInsumo --reporter=verbose
```

Salida esperada: `Cannot find module '../ModalCompraInsumo'` o similar.

---

## Task 2: Implementar ModalCompraInsumo

**Files:**
- Create: `Frontend/src/components/inventario/ModalCompraInsumo.jsx`

- [ ] **Step 3: Crear el componente**

```jsx
// Frontend/src/components/inventario/ModalCompraInsumo.jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { gastoService } from "../../services/gastoService";
import { fondoService } from "../../services/fondoService";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { formatDecimal } from "../../utils/format";
import { capFirst } from "../../utils/pedidoHelpers";

export default function ModalCompraInsumo({ insumos, onClose }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    insumo:   insumos[0]?.insumo ?? "gallina",
    cantidad: "",
    moneda:   "COP",
    monto:    "",
  });
  const [error,  setError]  = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn:  fondoService.listar,
  });
  const nequiCuentas = fondos.filter((f) => f.moneda.startsWith("NEQUI"));

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const stockActual = insumos.find((i) => i.insumo === form.insumo)?.stockActual ?? 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    const cantidad = parseInt(form.cantidad, 10);
    const monto    = Number(form.monto);
    if (!cantidad || cantidad < 1) { setError("La cantidad debe ser al menos 1"); return; }
    if (!monto    || monto   <= 0) { setError("El monto debe ser mayor a 0");     return; }

    const plural   = form.insumo === "cuy" ? "cuyes" : `${form.insumo}s`;
    const concepto = `Compra ${cantidad} ${plural}`;

    setSaving(true);
    try {
      await gastoService.registrar({
        concepto,
        categoria:           "insumos",
        moneda:              form.moneda,
        monto,
        insumoInventario:    form.insumo,
        cantidadInventario:  cantidad,
      });
      queryClient.invalidateQueries({ queryKey: ["inventario-stock"]      });
      queryClient.invalidateQueries({ queryKey: ["inventario-movimientos"] });
      queryClient.invalidateQueries({ queryKey: ["gastos"]                });
      queryClient.invalidateQueries({ queryKey: ["fondos"]                });
      onClose();
    } catch (err) {
      setError(err.message ?? "Error al registrar");
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar entrada de insumo</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="compra-insumo">Insumo</label>
                <select
                  id="compra-insumo"
                  className="form-control"
                  value={form.insumo}
                  onChange={(e) => set("insumo", e.target.value)}
                >
                  {insumos.map((i) => (
                    <option key={i.insumo} value={i.insumo}>{capFirst(i.insumo)}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="compra-cantidad">
                  Cantidad — Stock actual: <strong>{formatDecimal(stockActual)}</strong>
                </label>
                <input
                  id="compra-cantidad"
                  className="form-control"
                  type="number"
                  step="1"
                  min="1"
                  value={form.cantidad}
                  onChange={(e) => set("cantidad", e.target.value)}
                  autoFocus
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="compra-moneda">Moneda</label>
                <select
                  id="compra-moneda"
                  className="form-control"
                  value={form.moneda}
                  onChange={(e) => set("moneda", e.target.value)}
                >
                  <option value="COP">COP — Pesos</option>
                  <option value="USD">USD — Dólares</option>
                  {nequiCuentas.map((f) => (
                    <option key={f.moneda} value={f.moneda}>
                      {f.moneda}{f.alias ? ` — ${f.alias}` : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="compra-monto">
                  Monto {form.moneda === "USD" ? "(en dólares)" : "(en pesos)"}
                </label>
                <input
                  id="compra-monto"
                  className="form-control"
                  type="number"
                  min="0"
                  step={form.moneda === "USD" ? "0.01" : "100"}
                  value={form.monto}
                  onChange={(e) => set("monto", e.target.value)}
                />
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Registrar entrada"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Ejecutar los tests — deben PASAR**

```bash
cd Frontend && npm run test -- ModalCompraInsumo --reporter=verbose
```

Salida esperada: todos los tests en verde (`✓`). Si alguno falla, revisar que los `id` en el componente coincidan con los `getByLabelText` del test.

- [ ] **Step 5: Commit**

```bash
git add Frontend/src/components/inventario/ModalCompraInsumo.jsx \
        Frontend/src/components/inventario/__tests__/ModalCompraInsumo.test.jsx
git commit -m "feat(inventario): agregar ModalCompraInsumo con registro simultáneo de gasto"
```

---

## Task 3: Integrar botón y modal en InventarioPage

**Files:**
- Modify: `Frontend/src/pages/inventario/InventarioPage.jsx`

- [ ] **Step 6: Modificar InventarioPage**

Reemplazar el bloque completo de `InventarioPage.jsx` con:

```jsx
// Frontend/src/pages/inventario/InventarioPage.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, SlidersHorizontal } from "lucide-react";
import { inventarioService } from "../../services/inventarioService";
import { formatFecha } from "../../utils/format";
import { formatInsumo, capFirst } from "../../utils/pedidoHelpers";
import Pagination from "../../components/shared/Pagination";
import ModalCompraInsumo from "../../components/inventario/ModalCompraInsumo";

const PAGE_SIZE = 8;

const TIPO_CONFIG = {
  entrada: { label: "Entrada",  badge: "badge-success", icono: <ArrowDown size={13} /> },
  salida:  { label: "Salida",   badge: "badge-danger",  icono: <ArrowUp size={13} /> },
  ajuste:  { label: "Ajuste",   badge: "badge-warning", icono: <SlidersHorizontal size={13} /> },
};

export default function InventarioPage() {
  const navigate = useNavigate();
  const [filtroInsumo,       setFiltroInsumo]       = useState("");
  const [pagina,             setPagina]             = useState(0);
  const [modalCompraAbierto, setModalCompraAbierto] = useState(false);

  const { data: stock = [], isLoading: loadingStock } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn:  inventarioService.listarStock,
  });

  const { data: movimientos = [], isLoading: loadingMov } = useQuery({
    queryKey: ["inventario-movimientos", filtroInsumo],
    queryFn:  () => inventarioService.listarMovimientos(filtroInsumo || undefined),
  });

  const totalPaginas   = Math.max(1, Math.ceil(movimientos.length / PAGE_SIZE));
  const paginaActual   = Math.min(pagina, totalPaginas - 1);
  const movimientosPag = movimientos.slice(paginaActual * PAGE_SIZE, (paginaActual + 1) * PAGE_SIZE);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Inventario</h1>
        <button
          className="btn btn-primary"
          onClick={() => setModalCompraAbierto(true)}
        >
          + Registrar entrada
        </button>
      </div>

      {/* Stock actual */}
      <div className="kpi-grid mb-6">
        {loadingStock ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : stock.map((s) => {
          const borderCls = s.stockActual < 2
            ? "border-l-red-600"
            : s.stockActual < 5
            ? "border-l-amber-500"
            : "border-l-verde-claro";
          return (
            <div key={s.insumo} className={`kpi-card ${borderCls}`}>
              <div className="kpi-label">{capFirst(s.insumo)}</div>
              <div className="font-bold text-[18px] leading-snug mt-1">
                {formatInsumo(s.insumo, s.stockActual) ?? "0"}
              </div>
            </div>
          );
        })}
      </div>

      {/* Movimientos */}
      <div className="card">
        <div className="card-header">
          <span className="card-title shrink-0">Historial de movimientos</span>
          <div className="flex-1 flex justify-center">
            {movimientos.length > PAGE_SIZE && (
              <Pagination
                pagina={paginaActual}
                totalItems={movimientos.length}
                pageSize={PAGE_SIZE}
                onChange={setPagina}
              />
            )}
          </div>
          <div className="flex gap-2.5 items-center shrink-0">
            <select
              className="form-control w-40"
              value={filtroInsumo}
              onChange={(e) => { setFiltroInsumo(e.target.value); setPagina(0); }}
            >
              <option value="">Todos los insumos</option>
              {stock.map((s) => (
                <option key={s.insumo} value={s.insumo}>{capFirst(s.insumo)}</option>
              ))}
            </select>
          </div>
        </div>
        <div className="table-wrap">
          {loadingMov ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : movimientos.length === 0 ? (
            <div className="empty-state"><p>Sin movimientos registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha / Hora</th>
                  <th>Insumo</th>
                  <th>Tipo</th>
                  <th>Cantidad</th>
                  <th>Stock resultante</th>
                  <th>Referencia</th>
                  <th>Observación</th>
                </tr>
              </thead>
              <tbody>
                {movimientosPag.map((m) => {
                  const conf = TIPO_CONFIG[m.tipo] ?? { label: m.tipo, badge: "badge-gray", icono: null };
                  const esSalidaPedido = m.tipo === "salida" && m.referenciaTipo === "pedido" && m.referenciaId;
                  return (
                    <tr
                      key={m.id}
                      className={esSalidaPedido ? "cursor-pointer hover:bg-gray-50 transition-colors" : ""}
                      title={esSalidaPedido ? "Ver pedido en historial" : undefined}
                      onClick={esSalidaPedido
                        ? () => navigate("/pedidos", { state: { abrirPedidoId: Number(m.referenciaId), irAHistorial: true } })
                        : undefined}
                    >
                      <td className="text-gray-500 text-[12px] whitespace-nowrap">{formatFecha(m.registradoEn)}</td>
                      <td className="font-medium">{capFirst(m.insumo)}</td>
                      <td>
                        <span className={`badge ${conf.badge} gap-1`}>
                          {conf.icono} {conf.label}
                        </span>
                      </td>
                      <td className="font-semibold">
                        {m.tipo === "ajuste" ? "→ " : m.tipo === "entrada" ? "+ " : "− "}
                        {formatInsumo(m.insumo, Math.abs(m.cantidad)) ?? "0"}
                      </td>
                      <td>{formatInsumo(m.insumo, m.stockResultante) ?? "0"}</td>
                      <td className="text-gray-500 text-[12px]">
                        {m.referenciaTipo
                          ? `${capFirst(m.referenciaTipo)}${m.referenciaId ? " #" + m.referenciaId : ""}`
                          : "—"}
                      </td>
                      <td className="text-gray-500">{m.observacion ?? "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {modalCompraAbierto && (
        <ModalCompraInsumo
          insumos={stock}
          onClose={() => setModalCompraAbierto(false)}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 7: Ejecutar todos los tests — deben seguir pasando**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: todos los tests existentes en verde. Los tests de `ModalCompraInsumo` también deben pasar.

- [ ] **Step 8: Commit**

```bash
git add Frontend/src/pages/inventario/InventarioPage.jsx
git commit -m "feat(inventario): integrar botón Registrar entrada en InventarioPage"
```

---

## Task 4: Verificación manual en el navegador

- [ ] **Step 9: Levantar la app en desarrollo**

```bash
# Terminal 1 — Backend (si no está corriendo en Docker)
# El backend ya debe estar corriendo en :5020 (docker compose up)

# Terminal 2 — Frontend
cd Frontend && npm run dev
```

Abrir `http://localhost:5173` en el navegador.

- [ ] **Step 10: Verificar el flujo completo**

1. Navegar a **Inventario** → confirmar que aparece el botón **"+ Registrar entrada"** en el header
2. Hacer clic en el botón → debe abrirse el modal con 4 campos (Insumo, Cantidad, Moneda, Monto)
3. Seleccionar `Gallina`, cantidad `5`, moneda `COP`, monto `150000`
4. Clic en **"Registrar entrada"** → modal se cierra, los KPIs de stock se actualizan
5. Verificar en **Gastos** que aparece el gasto `"Compra 5 gallinas"` con categoría `insumos`
6. Volver a **Inventario** → el historial de movimientos muestra una entrada nueva con referencia `gasto`
7. Repetir con `Cuy`, cantidad `3` → verificar concepto `"Compra 3 cuyes"`
8. Intentar guardar con cantidad vacía → debe mostrar error `"La cantidad debe ser al menos 1"`
9. Intentar guardar con monto vacío → debe mostrar error `"El monto debe ser mayor a 0"`
