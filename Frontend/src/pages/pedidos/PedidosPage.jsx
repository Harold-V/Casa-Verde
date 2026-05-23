import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Plus, X, Trash2, CreditCard, CheckCircle, Ban,
  Search, Utensils, Wallet, Pencil, ChevronDown,
} from "lucide-react";
import { pedidoService } from "../../services/pedidoService";
import { productoService } from "../../services/productoService";
import { salonService } from "../../services/salonService";
import { formatCOP } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:          "badge-success",
  FINALIZADO:      "badge-info",
  ANULADO:         "badge-danger",
};

// Pedidos que aún están en operación (no cerrados ni anulados)
const ESTADOS_ACTIVOS = new Set(["PENDIENTE", "ABONADO_PARCIAL", "PAGADO"]);
const esActivo = (estado) => ESTADOS_ACTIVOS.has(estado);

// ── Input de dinero con formato de miles (COP/NEQUI) o decimal (USD) ─────────
function MoneyInput({ value, onChange, moneda = "COP", placeholder, className, autoFocus }) {
  const isCop = moneda !== "USD";

  const toDisplay = (raw) => {
    if (raw === "" || raw == null) return "";
    const digits = String(raw).replace(/\D/g, "");
    if (!digits) return "";
    const n = parseInt(digits, 10);
    return isNaN(n) ? "" : n.toLocaleString("es-CO"); // 100000 → "100.000"
  };

  const handleChange = (e) => {
    if (!isCop) { onChange(e.target.value); return; }
    onChange(e.target.value.replace(/\D/g, "")); // guardar solo dígitos
  };

  const handleKeyDown = (e) => {
    if (!isCop || (e.key !== "ArrowUp" && e.key !== "ArrowDown")) return;
    e.preventDefault();
    const cur = parseInt(String(value).replace(/\D/g, "") || "0", 10);
    onChange(String(e.key === "ArrowUp" ? cur + 1000 : Math.max(0, cur - 1000)));
  };

  return (
    <input
      type={isCop ? "text" : "number"}
      inputMode={isCop ? "numeric" : "decimal"}
      className={className}
      placeholder={placeholder}
      value={isCop ? toDisplay(value) : value}
      onChange={handleChange}
      onKeyDown={handleKeyDown}
      autoFocus={autoFocus}
      {...(!isCop && { step: "0.01", min: "0" })}
    />
  );
}

// ── Chip toggle para campos opcionales ────────────────────────────────────────
function ToggleChip({ label, active, onToggle }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className={`px-3 py-1 rounded-full text-xs font-semibold transition-colors cursor-pointer select-none
        ${active ? "bg-verde-claro text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"}`}
    >
      {label}
    </button>
  );
}

// ── Fila de producto con controles de cantidad ────────────────────────────────
function ProductoRow({ producto, cantidad, onChange }) {
  const activo = cantidad > 0;
  return (
    <div className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${activo ? "bg-verde-bg" : "hover:bg-gray-50"}`}>
      <div className="flex-1 min-w-0">
        <div className={`text-sm font-medium truncate ${activo ? "text-verde-oscuro font-semibold" : "text-gray-900"}`}>
          {producto.nombre}
        </div>
        <div className="text-[11px] text-gray-400">{formatCOP(producto.precio)}</div>
      </div>

      {cantidad === 0 ? (
        <button
          onClick={() => onChange(1)}
          className="w-7 h-7 rounded-full bg-verde-claro text-white flex items-center justify-center text-lg leading-none font-bold hover:bg-verde-oscuro transition-colors shrink-0"
        >
          +
        </button>
      ) : (
        <div className="flex items-center gap-0.5 bg-white border border-verde-claro rounded-full px-1 shrink-0">
          <button
            onClick={() => onChange(cantidad - 1)}
            className="w-6 h-6 flex items-center justify-center text-verde-oscuro font-bold text-lg leading-none hover:bg-gray-100 rounded-full transition-colors"
          >
            −
          </button>
          <span className="w-6 text-center text-sm font-bold text-verde-oscuro">{cantidad}</span>
          <button
            onClick={() => onChange(cantidad + 1)}
            className="w-6 h-6 flex items-center justify-center text-verde-oscuro font-bold text-lg leading-none hover:bg-gray-100 rounded-full transition-colors"
          >
            +
          </button>
        </div>
      )}
    </div>
  );
}

// ── Diálogo de confirmación genérico ──────────────────────────────────────────
function ConfirmDialog({ emoji, titulo, descripcion, labelOk, peligro = false, onOk, onCancelar }) {
  return (
    <div className="fixed inset-0 bg-black/60 z-[1100] flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl p-7 max-w-xs w-full text-center animate-slide-up">
        <div className="text-5xl mb-3">{emoji}</div>
        <h3 className="font-bold text-lg mb-1 text-gray-900">{titulo}</h3>
        <p className="text-gray-500 text-sm mb-6">{descripcion}</p>
        <div className="flex gap-3">
          <button className="btn btn-secondary flex-1" onClick={onCancelar}>Volver</button>
          <button className={`btn flex-1 ${peligro ? "btn-danger" : "btn-primary"}`} onClick={onOk}>
            {labelOk}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Overlay de éxito después de crear pedido ──────────────────────────────────
function PedidoCreado({ pedido, onVerPedido, onNuevoPedido, onCerrar }) {
  const totalItems = pedido.detalles?.length ?? 0;
  return (
    <div className="fixed inset-0 bg-black/50 z-[1000] flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden animate-slide-up">
        <div className="h-1.5 bg-verde-claro w-full" />
        <div className="p-8 text-center">
          <div className="w-16 h-16 rounded-full bg-verde-claro/10 border-2 border-verde-claro/20 flex items-center justify-center mx-auto mb-4">
            <CheckCircle size={36} className="text-verde-claro" />
          </div>
          <div className="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-1">
            Pedido registrado
          </div>
          <h2 className="text-2xl font-black text-gray-900 mb-1">
            {pedido.nombre ?? `Pedido #${pedido.id}`}
          </h2>
          <div className="text-sm text-gray-400 mb-3">
            #{pedido.id}
            {totalItems > 0 && ` · ${totalItems} ítem${totalItems !== 1 ? "s" : ""}`}
          </div>
          <div className="text-3xl font-black text-verde-oscuro mb-2">
            {formatCOP(pedido.totalCop)}
          </div>
          {pedido.totalAbonado > 0 && (
            <div className="text-sm text-verde-claro font-semibold mb-1">
              Abono registrado: {formatCOP(pedido.totalAbonado)}
            </div>
          )}
          {pedido.saldoPendiente > 0 && (
            <div className="text-sm text-red-500 font-semibold mb-1">
              Saldo pendiente: {formatCOP(pedido.saldoPendiente)}
            </div>
          )}
          <div className="flex flex-col gap-2.5 mt-6">
            <button className="btn btn-primary w-full justify-center py-2.5 text-[15px]" onClick={onVerPedido}>
              Ver pedido
            </button>
            <button className="btn btn-secondary w-full justify-center" onClick={onNuevoPedido}>
              <Plus size={14} /> Nuevo pedido
            </button>
            <button
              className="text-sm text-gray-400 hover:text-gray-600 cursor-pointer mt-1 transition-colors"
              onClick={onCerrar}
            >
              Listo, cerrar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Modal Crear pedido ────────────────────────────────────────────────────────
function ModalCrear({ salones, productos, onClose, onCrear }) {
  const [nombre, setNombre]     = useState("");
  const [on, setOn]             = useState({ fecha: false, hora: false, personas: false, salon: false });
  const [fecha, setFecha]       = useState(new Date().toISOString().slice(0, 10));
  const [hora, setHora]         = useState(new Date().toTimeString().slice(0, 5));
  const [personas, setPersonas] = useState(1);
  const [salonId, setSalonId]   = useState("");
  const [cantidades, setCantidades] = useState({});
  const [manual, setManual]     = useState({ nombre: "", precio: "" });
  const [manualItems, setManualItems] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [showAbono, setShowAbono]     = useState(false);
  const [abonoMoneda, setAbonoMoneda] = useState("COP");
  const [abonoMonto, setAbonoMonto]   = useState("");
  const [confirmCancelar, setConfirmCancelar] = useState(false);
  const [error, setError]       = useState("");
  const [saving, setSaving]     = useState(false);

  const tog = (k) => setOn((o) => {
    const next = { ...o, [k]: !o[k] };
    if (k === "fecha" && next.fecha) next.hora = true;
    if (k === "hora"  && !next.hora) next.fecha = false;
    return next;
  });

  const cambiarCantidad = (id, n) =>
    setCantidades((c) => {
      if (n <= 0) { const { [id]: _, ...rest } = c; return rest; }
      return { ...c, [id]: n };
    });

  const agregarManual = () => {
    if (!manual.nombre.trim()) return;
    setManualItems((ms) => [...ms, { ...manual, cantidad: 1, _key: Date.now() }]);
    setManual({ nombre: "", precio: "" });
  };

  const productosFiltrados = productos.filter((p) =>
    !busqueda || p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );

  const seleccionados = productos
    .filter((p) => (cantidades[p.id] ?? 0) > 0)
    .map((p) => ({ ...p, cantidad: cantidades[p.id] }));

  const totalItems = seleccionados.reduce((s, p) => s + p.cantidad, 0)
    + manualItems.reduce((s, m) => s + m.cantidad, 0);

  const totalEstimado = seleccionados.reduce((s, p) => s + p.precio * p.cantidad, 0)
    + manualItems.reduce((s, m) => s + (parseFloat(m.precio) || 0) * m.cantidad, 0);

  const handleCrear = async () => {
    if (!nombre.trim()) { setError("El nombre del cliente es obligatorio"); return; }
    if (totalItems === 0) { setError("Agrega al menos un ítem al pedido"); return; }
    setSaving(true); setError("");
    try {
      const detalles = [
        ...seleccionados.map((p) => ({ productoId: p.id, cantidad: p.cantidad })),
        ...manualItems.map((m) => ({
          nombreManual: m.nombre.trim(),
          precioManual: parseFloat(m.precio) || 0,
          cantidad: m.cantidad,
        })),
      ];
      await onCrear({
        nombre:       nombre.trim(),
        salonId:      on.salon && salonId ? Number(salonId) : null,
        fechaReserva: on.fecha   ? fecha   : null,
        horaReserva:  on.hora    ? hora    : null,
        cantPersonas: on.personas ? Number(personas) : null,
        abono:        showAbono && abonoMonto ? parseFloat(abonoMonto) : null,
        monedaAbono:  showAbono && abonoMonto ? abonoMoneda : null,
        detalles,
      });
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message);
      setSaving(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/45 z-[1000] flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl flex flex-col overflow-hidden animate-slide-up" style={{ height: "90vh" }}>

          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
            <h2 className="text-verde-claro font-extrabold text-xl">Nuevo Pedido</h2>
            <button className="btn-icon" onClick={() => setConfirmCancelar(true)}><X size={20} /></button>
          </div>

          {/* Body */}
          <div className="flex flex-1 overflow-hidden">

            {/* Izquierda: cliente + catálogo */}
            <div className="flex flex-col border-r border-gray-200 overflow-hidden" style={{ width: "58%" }}>
              <div className="px-5 py-3 border-b border-gray-100 shrink-0 space-y-2">
                {error && <div className="alert alert-error">{error}</div>}
                <input
                  className={`form-control text-[15px] font-semibold ${!nombre.trim() && error ? "border-red-500" : ""}`}
                  placeholder="Nombre del cliente *"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  autoFocus
                />
                <div className="flex flex-wrap gap-1.5">
                  <ToggleChip label="🕐 Reserva"   active={on.hora}     onToggle={() => tog("hora")} />
                  <ToggleChip label="👥 Personas"  active={on.personas} onToggle={() => tog("personas")} />
                  <ToggleChip label="🪑 Salón"     active={on.salon}    onToggle={() => tog("salon")} />
                </div>
                {(on.hora || on.personas || on.salon) && (
                  <div className="flex flex-wrap gap-2 pt-1 items-center">
                    {on.hora && (
                      <>
                        <input type="time" className="form-control min-w-[110px]" value={hora} onChange={(e) => setHora(e.target.value)} />
                        <ToggleChip label="📅 + Fecha" active={on.fecha} onToggle={() => tog("fecha")} />
                        {on.fecha && <input type="date" className="form-control flex-1 min-w-[140px]" value={fecha} onChange={(e) => setFecha(e.target.value)} />}
                      </>
                    )}
                    {on.personas && <input type="number" min="1" placeholder="Personas" className="form-control w-28" value={personas} onChange={(e) => setPersonas(e.target.value)} />}
                    {on.salon    && (
                      <select className="form-control flex-1" value={salonId} onChange={(e) => setSalonId(e.target.value)}>
                        <option value="">Salón…</option>
                        {salones.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
                      </select>
                    )}
                  </div>
                )}
              </div>

              <div className="px-4 py-2 border-b border-gray-100 shrink-0">
                <div className="relative">
                  <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                  <input
                    className="form-control pl-8 text-sm"
                    placeholder="Buscar producto…"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex-1 overflow-y-auto px-3 py-2">
                {productosFiltrados.length === 0 ? (
                  <div className="text-center py-8 text-gray-400 text-sm">Sin productos</div>
                ) : (
                  <div className="flex flex-col gap-0.5">
                    {productosFiltrados.map((p) => (
                      <ProductoRow
                        key={p.id}
                        producto={p}
                        cantidad={cantidades[p.id] ?? 0}
                        onChange={(n) => cambiarCantidad(p.id, n)}
                      />
                    ))}
                  </div>
                )}
                <div className="mt-4 pt-3 border-t border-gray-100">
                  <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-2">Ítem manual / no catalogado</div>
                  <div className="flex gap-2 items-center">
                    <input
                      className="form-control flex-1 text-sm"
                      placeholder="Descripción"
                      value={manual.nombre}
                      onChange={(e) => setManual((m) => ({ ...m, nombre: e.target.value }))}
                      onKeyDown={(e) => e.key === "Enter" && agregarManual()}
                    />
                    <input
                      className="form-control w-28 text-sm"
                      type="number" min="0" placeholder="Precio COP"
                      value={manual.precio}
                      onChange={(e) => setManual((m) => ({ ...m, precio: e.target.value }))}
                      onKeyDown={(e) => e.key === "Enter" && agregarManual()}
                    />
                    <button className="btn btn-secondary btn-sm shrink-0" onClick={agregarManual} title="Agregar ítem">
                      <Plus size={14} />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Derecha: resumen + abono */}
            <div className="flex-1 flex flex-col overflow-hidden bg-gray-50/40">
              <div className="px-5 py-3 border-b border-gray-200 shrink-0">
                <span className="font-bold text-sm text-gray-600">Resumen del pedido</span>
              </div>
              <div className="flex-1 overflow-y-auto px-4 py-3">
                {seleccionados.length === 0 && manualItems.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                    <Utensils size={44} className="mb-3 opacity-25" />
                    <p className="text-sm font-medium">Sin productos seleccionados</p>
                    <p className="text-xs mt-1">Usa el catálogo de la izquierda</p>
                  </div>
                ) : (
                  <div className="space-y-1">
                    {seleccionados.map((p) => (
                      <div key={p.id} className="flex items-center gap-2.5 py-1.5">
                        <span className="w-6 h-6 rounded-md bg-verde-claro text-white flex items-center justify-center text-[11px] font-bold shrink-0">{p.cantidad}</span>
                        <span className="flex-1 text-sm font-medium text-gray-800 truncate">{p.nombre}</span>
                        <span className="text-sm font-semibold text-verde-oscuro shrink-0">{formatCOP(p.precio * p.cantidad)}</span>
                      </div>
                    ))}
                    {manualItems.map((m) => (
                      <div key={m._key} className="flex items-center gap-2.5 py-1.5">
                        <span className="w-6 h-6 rounded-md bg-gray-400 text-white flex items-center justify-center text-[11px] font-bold shrink-0">{m.cantidad}</span>
                        <span className="flex-1 text-sm italic text-gray-600 truncate">{m.nombre}</span>
                        <span className="text-sm font-semibold shrink-0">{formatCOP((parseFloat(m.precio) || 0) * m.cantidad)}</span>
                        <button className="btn-icon text-red-400 shrink-0" onClick={() => setManualItems((ms) => ms.filter((x) => x._key !== m._key))}>
                          <X size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {totalItems > 0 && (
                <div className="border-t border-gray-200 bg-white shrink-0">
                  <div className="flex justify-between items-end px-5 pt-3 pb-2">
                    <div>
                      <div className="text-xs text-gray-400 mb-0.5">Total estimado</div>
                      <div className="text-[24px] font-black text-verde-oscuro leading-none">{formatCOP(totalEstimado)}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-gray-400">Ítems</div>
                      <div className="text-lg font-bold text-gray-700">{totalItems}</div>
                    </div>
                  </div>
                  <div className="px-5 pb-3">
                    {!showAbono ? (
                      <button className="btn btn-secondary btn-sm w-full justify-center text-gray-500" onClick={() => setShowAbono(true)}>
                        <Wallet size={13} /> Registrar abono inicial
                      </button>
                    ) : (
                      <div className="bg-gray-50 rounded-xl p-3 border border-gray-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-[12px] font-semibold text-verde-oscuro">Abono inicial</span>
                          <button className="btn-icon text-gray-400" onClick={() => { setShowAbono(false); setAbonoMonto(""); }}>
                            <X size={14} />
                          </button>
                        </div>
                        <div className="flex gap-2">
                          <select className="form-control text-sm w-28" value={abonoMoneda} onChange={(e) => setAbonoMoneda(e.target.value)}>
                            <option value="COP">COP</option>
                            <option value="USD">USD</option>
                            <option value="NEQUI">Nequi</option>
                          </select>
                          <MoneyInput
                            className="form-control flex-1 text-sm"
                            moneda={abonoMoneda}
                            placeholder="Monto"
                            value={abonoMonto}
                            onChange={setAbonoMonto}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white shrink-0">
            <button className="btn btn-secondary" onClick={() => setConfirmCancelar(true)}>Cancelar</button>
            <button
              className="btn btn-primary px-6 py-2.5 text-[15px] font-bold"
              onClick={handleCrear}
              disabled={saving || !nombre.trim()}
            >
              {saving ? "Creando…" : `🍽️ Crear pedido${totalItems > 0 ? ` · ${totalItems} ítem${totalItems !== 1 ? "s" : ""}` : ""}`}
            </button>
          </div>
        </div>
      </div>

      {confirmCancelar && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="¿Cancelar el pedido?"
          descripcion={totalItems > 0
            ? `Perderás ${totalItems} ítem${totalItems !== 1 ? "s" : ""} seleccionado${totalItems !== 1 ? "s" : ""}. No se creará ningún registro.`
            : "¿Seguro que deseas cerrar sin crear el pedido?"}
          labelOk="Sí, cancelar"
          peligro
          onOk={onClose}
          onCancelar={() => setConfirmCancelar(false)}
        />
      )}
    </>
  );
}

// ── Modal Agregar ítems a pedido existente (misma estética que ModalCrear) ────
function ModalAgregarItems({ productos, onConfirmar, onCancelar }) {
  const [cantidades, setCantidades] = useState({});
  const [busqueda, setBusqueda]     = useState("");
  const [manual, setManual]         = useState({ nombre: "", precio: "" });
  const [manualItems, setManualItems] = useState([]);
  const [error, setError]           = useState("");

  const cambiarCantidad = (id, n) =>
    setCantidades((c) => {
      if (n <= 0) { const { [id]: _, ...rest } = c; return rest; }
      return { ...c, [id]: n };
    });

  const agregarManual = () => {
    if (!manual.nombre.trim()) return;
    setManualItems((ms) => [...ms, { ...manual, cantidad: 1, _key: Date.now() }]);
    setManual({ nombre: "", precio: "" });
  };

  const productosFiltrados = productos.filter((p) =>
    !busqueda || p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );

  const seleccionados = productos
    .filter((p) => (cantidades[p.id] ?? 0) > 0)
    .map((p) => ({ ...p, cantidad: cantidades[p.id] }));

  const totalItems = seleccionados.reduce((s, p) => s + p.cantidad, 0)
    + manualItems.reduce((s, m) => s + m.cantidad, 0);

  const handleConfirmar = () => {
    if (totalItems === 0) { setError("Selecciona al menos un ítem"); return; }
    onConfirmar([
      ...seleccionados.map((p) => ({ productoId: p.id, cantidad: cantidades[p.id] })),
      ...manualItems.map((m) => ({
        nombreManual: m.nombre.trim(),
        precioManual: parseFloat(m.precio) || 0,
        cantidad: m.cantidad,
      })),
    ]);
  };

  return (
    /* z-[1050] — encima de ModalDetalle (1000), debajo de ConfirmDialog (1100) */
    <div className="fixed inset-0 bg-black/50 z-[1050] flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl flex flex-col overflow-hidden animate-slide-up" style={{ height: "85vh" }}>

        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
          <div>
            <h2 className="text-verde-claro font-extrabold text-xl">Agregar ítems al pedido</h2>
            <p className="text-[12px] text-gray-400 mt-0.5">Selecciona los productos a agregar y confirma al finalizar</p>
          </div>
          <button className="btn-icon" onClick={onCancelar}><X size={20} /></button>
        </div>

        {/* Body: dos columnas */}
        <div className="flex flex-1 overflow-hidden">

          {/* Izquierda: catálogo */}
          <div className="flex flex-col border-r border-gray-200 overflow-hidden" style={{ width: "58%" }}>
            <div className="px-4 py-2 border-b border-gray-100 shrink-0">
              <div className="relative">
                <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                <input
                  className="form-control pl-8 text-sm"
                  placeholder="Buscar producto…"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-3 py-2">
              {productosFiltrados.length === 0 ? (
                <div className="text-center py-8 text-gray-400 text-sm">Sin productos</div>
              ) : (
                <div className="flex flex-col gap-0.5">
                  {productosFiltrados.map((p) => (
                    <ProductoRow
                      key={p.id}
                      producto={p}
                      cantidad={cantidades[p.id] ?? 0}
                      onChange={(n) => cambiarCantidad(p.id, n)}
                    />
                  ))}
                </div>
              )}

              {/* Ítem manual */}
              <div className="mt-4 pt-3 border-t border-gray-100">
                <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-2">Ítem manual / no catalogado</div>
                <div className="flex gap-2 items-center">
                  <input
                    className="form-control flex-1 text-sm"
                    placeholder="Descripción"
                    value={manual.nombre}
                    onChange={(e) => setManual((m) => ({ ...m, nombre: e.target.value }))}
                    onKeyDown={(e) => e.key === "Enter" && agregarManual()}
                  />
                  <input
                    className="form-control w-28 text-sm"
                    type="number" min="0" placeholder="Precio COP"
                    value={manual.precio}
                    onChange={(e) => setManual((m) => ({ ...m, precio: e.target.value }))}
                    onKeyDown={(e) => e.key === "Enter" && agregarManual()}
                  />
                  <button className="btn btn-secondary btn-sm shrink-0" onClick={agregarManual} title="Agregar">
                    <Plus size={14} />
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Derecha: ítems a agregar */}
          <div className="flex-1 flex flex-col overflow-hidden bg-gray-50/40">
            <div className="px-5 py-3 border-b border-gray-200 shrink-0">
              <span className="font-bold text-sm text-gray-600">Ítems a agregar</span>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-3">
              {seleccionados.length === 0 && manualItems.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                  <Utensils size={44} className="mb-3 opacity-25" />
                  <p className="text-sm font-medium">Sin ítems seleccionados</p>
                  <p className="text-xs mt-1">Usa el catálogo de la izquierda</p>
                </div>
              ) : (
                <div className="space-y-1">
                  {seleccionados.map((p) => (
                    <div key={p.id} className="flex items-center gap-2.5 py-1.5">
                      <span className="w-6 h-6 rounded-md bg-verde-claro text-white flex items-center justify-center text-[11px] font-bold shrink-0">{p.cantidad}</span>
                      <span className="flex-1 text-sm font-medium text-gray-800 truncate">{p.nombre}</span>
                      <span className="text-sm font-semibold text-verde-oscuro shrink-0">{formatCOP(p.precio * p.cantidad)}</span>
                      <button
                        className="btn-icon text-red-400 shrink-0"
                        title="Quitar"
                        onClick={() => cambiarCantidad(p.id, 0)}
                      >
                        <X size={14} />
                      </button>
                    </div>
                  ))}
                  {manualItems.map((m) => (
                    <div key={m._key} className="flex items-center gap-2.5 py-1.5">
                      <span className="w-6 h-6 rounded-md bg-gray-400 text-white flex items-center justify-center text-[11px] font-bold shrink-0">{m.cantidad}</span>
                      <span className="flex-1 text-sm italic text-gray-600 truncate">{m.nombre}</span>
                      <span className="text-sm font-semibold shrink-0">{formatCOP((parseFloat(m.precio) || 0) * m.cantidad)}</span>
                      <button
                        className="btn-icon text-red-400 shrink-0"
                        onClick={() => setManualItems((ms) => ms.filter((x) => x._key !== m._key))}
                      >
                        <X size={14} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Totalizador */}
            {totalItems > 0 && (
              <div className="px-5 py-3 border-t border-gray-200 bg-white shrink-0">
                <div className="text-xs text-gray-400 mb-0.5">Ítems a agregar</div>
                <div className="text-lg font-black text-verde-oscuro">{totalItems} ítem{totalItems !== 1 ? "s" : ""}</div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white shrink-0">
          <div className="flex items-center gap-3">
            <button className="btn btn-secondary" onClick={onCancelar}>Cancelar</button>
            {error && <span className="text-red-500 text-sm">{error}</span>}
          </div>
          <button
            className="btn btn-primary px-6 py-2.5 text-[15px] font-bold"
            onClick={handleConfirmar}
            disabled={totalItems === 0}
          >
            {`✅ Confirmar · ${totalItems} ítem${totalItems !== 1 ? "s" : ""}`}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Modal Detalle / Editar (diseño dos columnas) ───────────────────────────────
function ModalDetalle({
  pedido, productos, onClose,
  onActualizar,
  onAgregarDetalle, onActualizarDetalle, onQuitarDetalle,
  onFinalizar, onAnular,
}) {
  const [showAgregarOverlay, setShowAgregarOverlay] = useState(false);
  const [editandoDetalleId, setEditandoDetalleId] = useState(null);
  const [editCantidad, setEditCantidad]         = useState("");
  const [loading, setLoading]                   = useState(false);
  const [error, setError]                       = useState("");
  const [confirm, setConfirm]                   = useState(null);
  // Edición de datos generales (reserva)
  const [showEditReserva, setShowEditReserva]   = useState(false);
  const [editReserva, setEditReserva]           = useState({
    on: { hora: !!pedido.horaReserva, fecha: !!pedido.fechaReserva },
    hora: pedido.horaReserva  ?? new Date().toTimeString().slice(0, 5),
    fecha: pedido.fechaReserva ?? new Date().toISOString().slice(0, 10),
  });
  const togReserva = (k) => setEditReserva((r) => ({
    ...r, on: { ...r.on, [k]: !r.on[k] },
  }));

  const abierto = esActivo(pedido.estado);
  const { totales: itotales, almuerzo: ialmuerzo } = calcularInsumosDetallado(pedido.detalles);

  const run = async (fn) => {
    setLoading(true); setError("");
    try { await fn(); }
    catch (err) { setError(err.response?.data?.mensaje ?? err.message); }
    finally { setLoading(false); }
  };

  const handleGuardarDetalle = (detalleId) => run(async () => {
    const cant = Number(editCantidad);
    if (isNaN(cant) || cant < 0) throw new Error("Cantidad inválida");
    if (cant === 0) {
      await onQuitarDetalle(detalleId);
    } else {
      await onActualizarDetalle(detalleId, { cantidad: cant });
    }
    setEditandoDetalleId(null);
  });

  const handleGuardarReserva = () => run(async () => {
    await onActualizar({
      fechaReserva: editReserva.on.fecha ? editReserva.fecha : null,
      horaReserva:  editReserva.on.hora  ? editReserva.hora  : null,
    });
    setShowEditReserva(false);
  });

  const handleConfirmarAgregarItems = async (items) => {
    setLoading(true); setError("");
    try {
      for (const req of items) await onAgregarDetalle(req); // serializado por item
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message);
    } finally {
      setLoading(false);
      setShowAgregarOverlay(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/45 z-[1000] flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl flex flex-col overflow-hidden animate-slide-up" style={{ maxHeight: "92vh" }}>

          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
            <div>
              <div className="flex items-center gap-2.5 flex-wrap">
                <span className="font-extrabold text-xl text-verde-claro">
                  {pedido.nombre ?? `Pedido #${pedido.id}`}
                </span>
                <span className={`badge ${ESTADO_BADGE[pedido.estado] ?? "badge-gray"}`}>
                  {pedido.estado}
                </span>
              </div>
              <div className="text-[12px] text-gray-500 flex gap-2.5 mt-0.5 flex-wrap">
                <span>#{pedido.id}</span>
                {pedido.salonNombre && <span>· {pedido.salonNombre}{pedido.mesa ? ` / ${pedido.mesa}` : ""}</span>}
                {pedido.cantPersonas && <span>· {pedido.cantPersonas} pers.</span>}
                {(pedido.fechaReserva || pedido.horaReserva) && (
                  <span className="flex items-center gap-1">
                    · 📅 {[
                      pedido.fechaReserva && formatFechaReserva(pedido.fechaReserva),
                      pedido.horaReserva  && formatHoraReserva(pedido.horaReserva),
                    ].filter(Boolean).join(" ")}
                    {abierto && (
                      <button className="btn-icon p-0 text-gray-400 hover:text-verde-oscuro" onClick={() => setShowEditReserva((v) => !v)}>
                        <Pencil size={10} />
                      </button>
                    )}
                  </span>
                )}
                {abierto && !(pedido.fechaReserva || pedido.horaReserva) && (
                  <button className="text-gray-400 hover:text-verde-oscuro text-[11px] underline underline-offset-2"
                    onClick={() => setShowEditReserva((v) => !v)}>
                    + Agregar reserva
                  </button>
                )}
              </div>
            </div>
            <button className="btn-icon" onClick={onClose}><X size={20} /></button>
          </div>

          {/* Panel edición de reserva */}
          {showEditReserva && abierto && (
            <div className="px-6 py-2.5 border-b border-gray-100 bg-gray-50/60 flex flex-wrap gap-2 items-center shrink-0 animate-slide-down">
              <ToggleChip label="🕐 Hora" active={editReserva.on.hora} onToggle={() => togReserva("hora")} />
              {editReserva.on.hora && (
                <input type="time" className="form-control min-w-[110px] text-sm"
                  value={editReserva.hora} onChange={(e) => setEditReserva((r) => ({ ...r, hora: e.target.value }))} />
              )}
              <ToggleChip label="📅 Fecha" active={editReserva.on.fecha} onToggle={() => togReserva("fecha")} />
              {editReserva.on.fecha && (
                <input type="date" className="form-control text-sm"
                  value={editReserva.fecha} onChange={(e) => setEditReserva((r) => ({ ...r, fecha: e.target.value }))} />
              )}
              <button className="btn btn-primary btn-sm" onClick={handleGuardarReserva} disabled={loading}>Guardar</button>
              <button className="btn-icon text-gray-400" onClick={() => setShowEditReserva(false)}><X size={14} /></button>
            </div>
          )}

          {/* Body: columna única */}
          <div className="flex flex-col flex-1 overflow-hidden">

            {/* Barra: título de ítems + botón Agregar */}
            <div className="flex items-center justify-between px-5 py-3 border-b border-gray-100 shrink-0">
              <span className="font-bold text-sm text-gray-600">Ítems · {pedido.detalles.length}</span>
              {abierto && (
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => { setShowAgregarOverlay(true); setError(""); }}
                >
                  <Plus size={13} /> Agregar
                </button>
              )}
            </div>

            {/* Panel de cocina */}
            {((itotales.gallina ?? 0) > 0 || (itotales.cuy ?? 0) > 0) && (
              <div className="px-5 py-2.5 border-b border-gray-100 bg-green-600/[0.05] shrink-0">
                <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">Cocina</div>
                <div className="flex gap-5 flex-wrap">
                  {(itotales.gallina ?? 0) > 0 && (
                    <div>
                      <span className="text-[13px] font-semibold text-gray-800">
                        🐔 {formatInsumo("gallina", itotales.gallina)}
                      </span>
                      {(ialmuerzo.gallina ?? 0) > 0 && (
                        <div className="text-[11px] text-gray-500 mt-0.5">
                          ↳ {formatInsumo("gallina", ialmuerzo.gallina)} en almuerzos
                        </div>
                      )}
                    </div>
                  )}
                  {(itotales.cuy ?? 0) > 0 && (
                    <div>
                      <span className="text-[13px] font-semibold text-gray-800">
                        🐾 {formatInsumo("cuy", itotales.cuy)}
                      </span>
                      {(ialmuerzo.cuy ?? 0) > 0 && (
                        <div className="text-[11px] text-gray-500 mt-0.5">
                          ↳ {formatInsumo("cuy", ialmuerzo.cuy)} en almuerzos
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Lista de ítems — scrollable */}
            <div className="flex-1 overflow-y-auto px-5 py-3">
              {error && <div className="alert alert-error mb-3">{error}</div>}
              {pedido.detalles.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                  <Utensils size={44} className="mb-3 opacity-25" />
                  <p className="text-sm font-medium">Sin ítems</p>
                  {abierto && <p className="text-xs mt-1">Usa "Agregar" para añadir productos</p>}
                </div>
              ) : (
                <div className="flex flex-col gap-1.5">
                  {pedido.detalles.map((d, i) => (
                    editandoDetalleId === d.id ? (
                      /* Fila en modo edición */
                      <div key={d.id} className="flex items-center gap-2 px-3 py-2.5 rounded-lg bg-verde-bg border border-verde-claro/30 animate-slide-down">
                        <input
                          type="number" min="0"
                          className="form-control w-16 text-sm text-center font-bold"
                          value={editCantidad}
                          onChange={(e) => setEditCantidad(e.target.value)}
                          autoFocus
                          onKeyDown={(e) => e.key === "Enter" && handleGuardarDetalle(d.id)}
                        />
                        <div className="flex-1 text-[14px] font-semibold text-gray-700 truncate">{d.nombreSnapshot}</div>
                        <button type="button" className="btn btn-primary btn-sm shrink-0"
                          onClick={() => handleGuardarDetalle(d.id)} disabled={loading}>Guardar</button>
                        <button type="button" className="btn-icon text-red-500 shrink-0" title="Eliminar ítem"
                          disabled={loading}
                          onClick={() => run(async () => { await onQuitarDetalle(d.id); setEditandoDetalleId(null); })}>
                          <Trash2 size={14} />
                        </button>
                        <button type="button" className="btn-icon text-gray-400 shrink-0"
                          onClick={() => { setEditandoDetalleId(null); setError(""); }}>
                          <X size={14} />
                        </button>
                      </div>
                    ) : (
                      /* Fila normal */
                      <div key={d.id}
                        className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg border border-gray-200 ${i % 2 === 0 ? "bg-gray-50" : "bg-white"}`}>
                        <div className="min-w-[28px] h-7 rounded-md bg-verde-claro text-white flex items-center justify-center font-extrabold text-[13px] shrink-0">
                          {d.cantidad}
                        </div>
                        <div className="flex-1 font-semibold text-[14px]">
                          {d.nombreSnapshot}
                          {d.precioUnitarioCop > 0 && (
                            <span className="font-normal text-[12px] text-gray-500 ml-2">{formatCOP(d.precioUnitarioCop)} c/u</span>
                          )}
                        </div>
                        <div className="font-bold text-[14px] text-verde-oscuro shrink-0">{formatCOP(d.subtotalCop)}</div>
                        {abierto && (
                          <button
                            className="btn-icon text-gray-400 hover:text-verde-oscuro shrink-0"
                            title="Editar cantidad" disabled={loading}
                            onClick={() => { setEditandoDetalleId(d.id); setEditCantidad(String(d.cantidad)); setError(""); }}>
                            <Pencil size={14} />
                          </button>
                        )}
                      </div>
                    )
                  ))}
                </div>
              )}
              {pedido.observacion && (
                <div className="mt-4 text-[13px] text-gray-600 bg-yellow-50 rounded-lg px-3 py-2 border border-yellow-100">
                  <strong>Obs.:</strong> {pedido.observacion}
                </div>
              )}
            </div>

            {/* KPIs: total / pagado / saldo */}
            <div className="grid grid-cols-3 gap-3 px-5 py-3 border-t border-gray-100 bg-gray-50/70 shrink-0">
              <div className="kpi-card" style={{ padding: "10px 12px" }}>
                <div className="kpi-label text-[10px]">Total</div>
                <div className="kpi-value text-[15px]">{formatCOP(pedido.totalCop)}</div>
              </div>
              <div className="kpi-card border-l-verde-claro" style={{ padding: "10px 12px" }}>
                <div className="kpi-label text-[10px]">Pagado</div>
                <div className="kpi-value text-[15px] text-verde-claro">{formatCOP(pedido.totalAbonado)}</div>
              </div>
              {(pedido.valorCambio ?? 0) > 0 ? (
                <div className="kpi-card border-l-amber-500" style={{ padding: "10px 12px" }}>
                  <div className="kpi-label text-[10px]">Cambio</div>
                  <div className="kpi-value text-[15px] text-amber-600">{formatCOP(pedido.valorCambio)}</div>
                </div>
              ) : (
                <div className={`kpi-card ${pedido.saldoPendiente > 0 ? "border-l-red-600" : "border-l-verde-claro"}`} style={{ padding: "10px 12px" }}>
                  <div className="kpi-label text-[10px]">Saldo</div>
                  <div className={`kpi-value text-[15px] ${pedido.saldoPendiente > 0 ? "text-red-600" : "text-verde-claro"}`}>
                    {formatCOP(pedido.saldoPendiente)}
                  </div>
                </div>
              )}
            </div>

            {/* Acciones fijas al fondo */}
            <div className="flex items-center justify-between gap-2 px-5 py-4 border-t border-gray-200 bg-white shrink-0">
              <div>
                {pedido.estado !== "ANULADO" && pedido.estado !== "FINALIZADO" && (
                  <button type="button" className="btn btn-danger"
                    onClick={() => setConfirm({ tipo: "anular" })} disabled={loading}>
                    <Ban size={15} /> Anular
                  </button>
                )}
              </div>
              <div className="flex gap-2">
                {abierto && pedido.saldoPendiente <= 0 && pedido.detalles.length > 0 && (
                  <button type="button" className="btn btn-primary bg-verde-oscuro hover:bg-[#0f2b1e]"
                    onClick={() => setConfirm({ tipo: "finalizar" })} disabled={loading}>
                    <CheckCircle size={15} /> Finalizar
                  </button>
                )}
                <button type="button" className="btn btn-secondary" onClick={onClose}>Cerrar</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Overlay de agregar ítems — encima del modal de detalle */}
      {showAgregarOverlay && (
        <ModalAgregarItems
          productos={productos}
          onConfirmar={handleConfirmarAgregarItems}
          onCancelar={() => setShowAgregarOverlay(false)}
        />
      )}

      {/* Confirmación de finalizar / anular */}
      {confirm && (
        <ConfirmDialog
          emoji={confirm.tipo === "anular" ? "🚫" : "✅"}
          titulo={confirm.tipo === "anular" ? "¿Anular el pedido?" : "¿Finalizar el pedido?"}
          descripcion={
            confirm.tipo === "anular"
              ? "Esta acción no se puede deshacer."
              : "Se descontará el inventario correspondiente al cerrar el pedido."
          }
          labelOk={confirm.tipo === "anular" ? "Sí, anular" : "Sí, finalizar"}
          peligro={confirm.tipo === "anular"}
          onOk={() => { run(confirm.tipo === "anular" ? onAnular : onFinalizar); setConfirm(null); }}
          onCancelar={() => setConfirm(null)}
        />
      )}
    </>
  );
}

// ── Helpers de insumos ─────────────────────────────────────────────────────────
function calcularInsumos(detalles = []) {
  const totales = {};
  for (const d of detalles) {
    if (!d.composicionSnapshot) continue;
    try {
      for (const { insumo, cantidad } of JSON.parse(d.composicionSnapshot)) {
        totales[insumo] = (totales[insumo] ?? 0) + cantidad * d.cantidad;
      }
    } catch {}
  }
  return totales;
}

// Igual que calcularInsumos pero además separa cuánto va en "almuerzos"
// (ítems que tienen AMBOS: gallina y cuy en su composición)
function calcularInsumosDetallado(detalles = []) {
  const totales  = {};
  const almuerzo = {};
  for (const d of detalles) {
    if (!d.composicionSnapshot) continue;
    try {
      const comp = JSON.parse(d.composicionSnapshot);
      const esAlmuerzo = comp.some((c) => c.insumo === "gallina") &&
                         comp.some((c) => c.insumo === "cuy");
      for (const { insumo, cantidad } of comp) {
        totales[insumo]  = (totales[insumo]  ?? 0) + cantidad * d.cantidad;
        if (esAlmuerzo)
          almuerzo[insumo] = (almuerzo[insumo] ?? 0) + cantidad * d.cantidad;
      }
    } catch {}
  }
  return { totales, almuerzo };
}

// Unidad mínima por insumo (= 1 presa)
const INSUMO_UNIDAD = { gallina: 0.125, cuy: 0.25 };

// Capitaliza el primer carácter alfabético del string → "1 cuy" → "1 Cuy"
const capFirst = (s) => s.replace(/[a-záéíóúñ]/i, (c) => c.toUpperCase());

// Devuelve array de partes textuales para el fraccionario n (sin " + " interno)
function fracParts(insumo, n) {
  const unidad = INSUMO_UNIDAD[insumo];
  if (n >= 0.5 - 0.001) {
    const resto  = Math.round((n - 0.5) * 1000) / 1000;
    const mitad  = `mitad de ${insumo}`;
    if (resto < 0.001) return [mitad];
    if (!unidad)       return [mitad, `${parseFloat(resto.toFixed(3))} ${insumo}`];
    const presas = Math.round(resto / unidad);
    if (presas === 0)  return [mitad];
    return [mitad, `${presas} ${presas === 1 ? "presa" : "presas"} de ${insumo}`];
  }
  if (!unidad) return [`${parseFloat(n.toFixed(3))} ${insumo}`];
  const presas = Math.round(n / unidad);
  return [`${presas} ${presas === 1 ? "presa" : "presas"} de ${insumo}`];
}

function formatInsumo(insumo, total) {
  if (total <= 0) return null;
  const plural   = insumo === "cuy" ? "cuyes" : `${insumo}s`;
  const intPart  = Math.floor(total);
  const fracPart = Math.round((total - intPart) * 1000) / 1000;
  const parts    = [];
  if (intPart  > 0)     parts.push(`${intPart} ${intPart === 1 ? insumo : plural}`);
  if (fracPart > 0.001) parts.push(...fracParts(insumo, fracPart));
  return parts.map(capFirst).join(" + ");
}

// "2026-05-23" → "sábado 23 de mayo"  (sin año si es el año actual)
//             → "sábado 23 de mayo de 2027"  (con año si difiere)
function formatFechaReserva(fechaStr) {
  if (!fechaStr) return null;
  const [y, m, d] = fechaStr.split("-").map(Number);
  const date = new Date(y, m - 1, d); // fecha local sin problemas de zona horaria
  const mismoAnio = y === new Date().getFullYear();
  const diaSemana = capFirst(date.toLocaleDateString("es-CO", { weekday: "long" }));
  const mes       = date.toLocaleDateString("es-CO", { month: "long" });
  return mismoAnio
    ? `${diaSemana} ${d} de ${mes}`
    : `${diaSemana} ${d} de ${mes} de ${y}`;
}

// "07:00" → "7:00 AM" · "15:07" → "3:07 PM"
function formatHoraReserva(h) {
  if (!h) return null;
  const [hh, mm] = h.split(":").map(Number);
  const period = hh >= 12 ? "PM" : "AM";
  const h12    = (hh % 12) || 12;
  return `${h12}:${String(mm).padStart(2, "0")} ${period}`;
}

// ── Modal Cobrar (factura rápida + pago) ──────────────────────────────────────
function ModalCobrar({ pedido, onClose, onPago, onActualizarPago, onQuitarPago, onFinalizar }) {
  const { formatFecha } = useFormatos();
  const [pagoForm, setPagoForm]             = useState({ moneda: "COP", monto: "" });
  const [showPagoForm, setShowPagoForm]     = useState(false);
  const [editandoPagoId, setEditandoPagoId] = useState(null);
  const [editPagoForm, setEditPagoForm]     = useState({ moneda: "COP", monto: "" });
  const [loading, setLoading]               = useState(false);
  const [error, setError]                   = useState("");
  const [confirm, setConfirm]               = useState(false);
  const abierto = esActivo(pedido.estado);

  const run = async (fn) => {
    setLoading(true); setError("");
    try { await fn(); }
    catch (e) { setError(e.response?.data?.mensaje ?? e.message); }
    finally { setLoading(false); }
  };

  const handlePago = () => run(async () => {
    if (!pagoForm.monto || Number(pagoForm.monto) <= 0) throw new Error("El monto debe ser mayor a 0");
    await onPago({ moneda: pagoForm.moneda, monto: Number(pagoForm.monto) });
    setPagoForm({ moneda: "COP", monto: "" }); setShowPagoForm(false);
  });

  const handleGuardarPago = (pagoId) => run(async () => {
    const monto = Number(editPagoForm.monto);
    if (isNaN(monto) || monto < 0) throw new Error("Monto inválido");
    if (monto === 0) {
      await onQuitarPago(pagoId);
    } else {
      await onActualizarPago(pagoId, { moneda: editPagoForm.moneda, monto });
    }
    setEditandoPagoId(null);
  });

  return (
    <>
      <div className="fixed inset-0 bg-black/45 z-[1000] flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md flex flex-col overflow-hidden animate-slide-up" style={{ maxHeight: "92vh" }}>

          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
            <div>
              <h2 className="font-extrabold text-xl text-verde-claro">{pedido.nombre}</h2>
              <div className="text-[12px] text-gray-500 mt-0.5 flex gap-2 flex-wrap">
                <span>#{pedido.id}</span>
                {pedido.salonNombre && <span>· {pedido.salonNombre}</span>}
                {(pedido.fechaReserva || pedido.horaReserva) && (
                  <span>· 📅 {[
                    pedido.fechaReserva && formatFechaReserva(pedido.fechaReserva),
                    pedido.horaReserva  && formatHoraReserva(pedido.horaReserva),
                  ].filter(Boolean).join(" ")}</span>
                )}
              </div>
            </div>
            <button className="btn-icon" onClick={onClose}><X size={20} /></button>
          </div>

          <div className="flex-1 overflow-y-auto">
            <div className="px-6 pt-4 pb-2">
              <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Factura</div>
              {pedido.detalles.map((d) => (
                <div key={d.id} className="flex justify-between py-1.5 border-b border-gray-100 text-[13px]">
                  <span className="text-gray-700">
                    <span className="font-bold text-verde-oscuro">{d.cantidad}×</span>{" "}
                    {d.nombreSnapshot}
                    {d.precioUnitarioCop > 0 && <span className="text-gray-400 ml-1">{formatCOP(d.precioUnitarioCop)}</span>}
                  </span>
                  {d.subtotalCop > 0 && <span className="font-semibold">{formatCOP(d.subtotalCop)}</span>}
                </div>
              ))}
            </div>

            <div className="px-6 pb-3 border-b border-gray-200">
              <div className="flex justify-between py-1 text-[13px] text-gray-500">
                <span>Subtotal</span><span className="font-semibold text-gray-800">{formatCOP(pedido.totalCop)}</span>
              </div>
              <div className="flex justify-between py-1 text-[13px] text-verde-claro">
                <span>Pagado</span><span className="font-semibold">{formatCOP(pedido.totalAbonado)}</span>
              </div>
              <div className={`flex justify-between py-1.5 font-bold text-[16px] ${pedido.saldoPendiente > 0 ? "text-red-600" : "text-verde-claro"}`}>
                <span>Saldo</span><span>{formatCOP(pedido.saldoPendiente)}</span>
              </div>
              {(pedido.valorCambio ?? 0) > 0 && (
                <div className="flex justify-between py-1 font-semibold text-[14px] text-amber-600 border-t border-amber-100 mt-1 pt-1">
                  <span>💵 Cambio</span><span>{formatCOP(pedido.valorCambio)}</span>
                </div>
              )}
            </div>

            {pedido.pagos.length > 0 && (
              <div className="px-6 py-3 border-b border-gray-200">
                <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Cobros · {pedido.pagos.length}</div>
                <div className="flex flex-col gap-1.5">
                  {pedido.pagos.map((p) => {
                    const editando = editandoPagoId === p.id;
                    return editando ? (
                      <div key={p.id} className="px-3 py-2 rounded-lg bg-verde-bg border border-verde-claro/30 animate-slide-down">
                        <div className="flex gap-2">
                          <select className="form-control text-sm w-24" value={editPagoForm.moneda}
                            onChange={(e) => setEditPagoForm((f) => ({ ...f, moneda: e.target.value }))}>
                            <option value="COP">COP</option><option value="USD">USD</option><option value="NEQUI">Nequi</option>
                          </select>
                          <MoneyInput className="form-control flex-1 text-sm" moneda={editPagoForm.moneda}
                            placeholder={editPagoForm.moneda === "USD" ? "Dólares" : "Pesos"}
                            value={editPagoForm.monto} onChange={(v) => setEditPagoForm((f) => ({ ...f, monto: v }))} autoFocus />
                          <button type="button" className="btn btn-primary btn-sm" onClick={() => handleGuardarPago(p.id)} disabled={loading}>Guardar</button>
                          <button type="button" className="btn-icon text-red-500 shrink-0" title="Eliminar cobro" disabled={loading}
                            onClick={() => run(async () => { await onQuitarPago(p.id); setEditandoPagoId(null); })}>
                            <Trash2 size={14} />
                          </button>
                          <button type="button" className="btn-icon text-gray-400" onClick={() => setEditandoPagoId(null)}><X size={14} /></button>
                        </div>
                      </div>
                    ) : (
                      <div key={p.id} className="flex items-center gap-2 text-[13px]">
                        <span className={`badge shrink-0 ${p.moneda === "USD" ? "badge-warning" : p.moneda === "NEQUI" ? "badge-gray" : "badge-success"}`}>{p.moneda}</span>
                        <span className="flex-1 font-semibold">{formatCOP(p.montoCop)}</span>
                        <span className="text-[11px] text-gray-400">{formatFecha(p.registradoEn)}</span>
                        {abierto && (
                          <button className="btn-icon text-gray-400 hover:text-verde-oscuro" onClick={() => { setEditandoPagoId(p.id); setEditPagoForm({ moneda: p.moneda, monto: String(p.monto) }); }}>
                            <Pencil size={12} />
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {abierto && (
              <div className="px-6 py-3">
                {error && <div className="alert alert-error mb-2 text-sm">{error}</div>}
                {!showPagoForm ? (
                  <button className="btn btn-secondary w-full justify-center" onClick={() => { setShowPagoForm(true); setError(""); }}>
                    <CreditCard size={14} /> Registrar cobro
                  </button>
                ) : (
                  <div className="bg-gray-50 rounded-xl p-3 border border-gray-200 animate-slide-down">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-[12px] font-semibold text-verde-oscuro">Nuevo cobro</span>
                      <button className="btn-icon text-gray-400" onClick={() => { setShowPagoForm(false); setError(""); }}><X size={14} /></button>
                    </div>
                    <div className="flex gap-2">
                      <select className="form-control text-sm w-28" value={pagoForm.moneda} onChange={(e) => setPagoForm((f) => ({ ...f, moneda: e.target.value }))}>
                        <option value="COP">COP</option><option value="USD">USD</option><option value="NEQUI">Nequi</option>
                      </select>
                      <MoneyInput className="form-control flex-1 text-sm" moneda={pagoForm.moneda}
                        placeholder={pagoForm.moneda === "USD" ? "Dólares" : "Pesos COP"}
                        value={pagoForm.monto} onChange={(v) => setPagoForm((f) => ({ ...f, monto: v }))} autoFocus />
                      <button type="button" className="btn btn-primary btn-sm" onClick={handlePago} disabled={loading}>Cobrar</button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className={`flex items-center gap-2 px-6 py-4 border-t border-gray-200 bg-white shrink-0 ${abierto && pedido.saldoPendiente <= 0 && pedido.detalles.length > 0 ? "justify-between" : "justify-end"}`}>
            <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
            {abierto && pedido.saldoPendiente <= 0 && pedido.detalles.length > 0 && (
              <button className="btn btn-primary bg-verde-oscuro hover:bg-[#0f2b1e]" onClick={() => setConfirm(true)} disabled={loading}>
                <CheckCircle size={15} /> Finalizar pedido
              </button>
            )}
          </div>
        </div>
      </div>

      {confirm && (
        <ConfirmDialog emoji="✅" titulo="¿Finalizar el pedido?"
          descripcion="Se descontará el inventario correspondiente."
          labelOk="Sí, finalizar"
          onOk={() => { run(onFinalizar); setConfirm(false); }}
          onCancelar={() => setConfirm(false)} />
      )}
    </>
  );
}

// ── Tarjeta de pedido activo ───────────────────────────────────────────────────
function OrdenCard({ pedido, onEditar, onCobrar, idx = 0 }) {
  const { formatHora } = useFormatos();
  const [expandido, setExpandido] = useState(false);
  const saldo        = pedido.saldoPendiente ?? 0;
  const abierto      = pedido.estado === "ABIERTO";
  const insumos      = abierto ? calcularInsumos(pedido.detalles) : {};
  const gallina      = insumos["gallina"];
  const cuy          = insumos["cuy"];
  const tieneReserva = !!(pedido.fechaReserva || pedido.horaReserva);
  const nItems       = pedido.detalles?.length ?? 0;

  return (
    /* Wrapper relativo: el grid cell tiene su altura normal; el dropdown flota fuera */
    <div className="relative h-full animate-fade-in-card" style={{ animationDelay: `${Math.min(idx * 25, 120)}ms` }}>
      <div className="card overflow-hidden p-0 transition-shadow duration-150 hover:shadow-lg flex flex-col h-full">
        <div className="p-[18px] cursor-pointer select-none" onClick={() => setExpandido((v) => !v)}>
          <div className="flex justify-between items-start mb-0.5 gap-2">
            <span className="font-bold text-[15px] truncate">
              {pedido.nombre ?? `#${pedido.id}`}
              <span className="ml-1.5 text-gray-500 text-[12px] font-normal">#{pedido.id}</span>
            </span>
            <div className="flex items-center gap-1 shrink-0">
              {tieneReserva && (
                <span className="text-[9px] font-bold px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-700 border border-amber-200">
                  RESERVA
                </span>
              )}
              <span className={`badge ${ESTADO_BADGE[pedido.estado] ?? "badge-gray"}`}>{pedido.estado}</span>
            </div>
          </div>

          {abierto && (pedido.fechaReserva || pedido.horaReserva) && (
            <div className="font-extrabold text-[17px] text-verde-oscuro mb-1">
              {pedido.fechaReserva
                ? formatFechaReserva(pedido.fechaReserva) + (pedido.horaReserva ? ` · ${formatHoraReserva(pedido.horaReserva)}` : "")
                : formatHoraReserva(pedido.horaReserva)
              }
            </div>
          )}

          <div className="text-[12px] text-gray-500 mb-2.5 flex gap-2 flex-wrap">
            {pedido.salonNombre && <span>{pedido.salonNombre}{pedido.mesa ? ` / ${pedido.mesa}` : ""}</span>}
            {pedido.cantPersonas && <span>· {pedido.cantPersonas} pers.</span>}
            {abierto && !tieneReserva && <span>{formatHora(pedido.creadoEn)}</span>}
            {!abierto && pedido.fechaReserva && (
              <span>{formatFechaReserva(pedido.fechaReserva)}{pedido.horaReserva ? ` ${formatHoraReserva(pedido.horaReserva)}` : ""}</span>
            )}
          </div>

          {abierto && (gallina || cuy) && (
            <div className="flex gap-2.5 mb-2.5 px-2.5 py-[5px] rounded-md bg-green-600/[0.07] border border-green-600/20 text-[13px] font-semibold">
              {gallina && <span>🐔 {formatInsumo("gallina", gallina)}</span>}
              {cuy     && <span>🐾 {formatInsumo("cuy", cuy)}</span>}
            </div>
          )}

          <div className="flex gap-4 items-end">
            <div>
              <div className="text-[11px] text-gray-500">Total</div>
              <div className="font-bold text-[14px]">{formatCOP(pedido.totalCop)}</div>
            </div>
            <div>
              <div className="text-[11px] text-gray-500">Saldo</div>
              <div className={`font-bold text-[14px] ${saldo > 0 ? "text-red-600" : "text-verde-claro"}`}>{formatCOP(saldo)}</div>
            </div>
            <button
              className="ml-auto flex items-center gap-1 text-[12px] text-gray-500 hover:text-verde-oscuro transition-colors"
              onClick={(e) => { e.stopPropagation(); setExpandido((v) => !v); }}
            >
              {nItems} ítem{nItems !== 1 ? "s" : ""}
              <ChevronDown size={13} className={`transition-transform duration-200 ${expandido ? "rotate-180" : ""}`} />
            </button>
          </div>
        </div>

        {abierto && (
          <div className="flex border-t border-gray-100 mt-auto">
            <button
              className="flex-1 flex items-center justify-center gap-1.5 py-2.5 text-[13px] font-medium text-gray-500 hover:bg-gray-50 transition-colors"
              onClick={(e) => { e.stopPropagation(); onEditar(pedido); }}
            >
              <Pencil size={13} /> Editar
            </button>
            {pedido.totalCop > 0 && (
              <>
                <div className="w-px bg-gray-100" />
                <button
                  className="flex-1 flex items-center justify-center gap-1.5 py-2.5 text-[13px] font-semibold text-verde-claro hover:bg-verde-claro hover:text-white transition-colors rounded-br-xl"
                  onClick={(e) => { e.stopPropagation(); onCobrar(pedido); }}
                >
                  <CreditCard size={13} /> Cobrar
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Dropdown flotante de ítems — fuera del flujo del grid */}
      {expandido && nItems > 0 && (
        <div className="absolute left-0 right-0 z-30 bg-white border border-gray-200 border-t-0 rounded-b-xl shadow-xl animate-slide-down"
             style={{ top: "100%" }}>
          <div className="px-4 py-2">
            {pedido.detalles.map((d) => (
              <div key={d.id} className="flex justify-between py-1 text-[13px] border-b border-gray-50 last:border-0">
                <span className="text-gray-700">
                  <span className="font-bold text-verde-oscuro mr-1">{d.cantidad}×</span>
                  {d.nombreSnapshot}
                </span>
                {d.subtotalCop > 0 && <span className="font-semibold text-gray-800 shrink-0 ml-2">{formatCOP(d.subtotalCop)}</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// Sin reserva = cliente ya en el establecimiento → primero (desc creadoEn)
// Con reserva = evento futuro → después, ordenado del más próximo al más lejano
function sortByReserva(list) {
  const today = new Date().toISOString().slice(0, 10);
  return [...list].sort((a, b) => {
    const ts = (p) => {
      if (p.fechaReserva && p.horaReserva) return new Date(`${p.fechaReserva}T${p.horaReserva}`).getTime();
      if (p.fechaReserva)                  return new Date(`${p.fechaReserva}T00:00`).getTime();
      if (p.horaReserva)                   return new Date(`${today}T${p.horaReserva}`).getTime();
      return null;
    };
    const ta = ts(a), tb = ts(b);
    if (ta == null && tb == null) return new Date(b.creadoEn) - new Date(a.creadoEn); // ambos sin reserva → más reciente primero
    if (ta == null) return -1;  // a sin reserva → a primero (atendiendo ahora)
    if (tb == null) return  1;  // b sin reserva → b primero (atendiendo ahora)
    return ta - tb;             // ambos con reserva → más próximo primero
  });
}

// ── Página principal ───────────────────────────────────────────────────────────
export default function PedidosPage() {
  const qc = useQueryClient();
  const [showCrear, setShowCrear]                   = useState(false);
  const [pedidoActivo, setPedidoActivo]             = useState(null);
  const [pedidoCobrar, setPedidoCobrar]             = useState(null);
  const [pedidoCreadoExito, setPedidoCreadoExito]   = useState(null);
  const [tab, setTab]                               = useState("activos");
  const [busqueda, setBusqueda]                     = useState("");

  const verTodos = tab === "historial";

  const { data: pedidos = [], isLoading } = useQuery({
    queryKey: ["pedidos", verTodos],
    queryFn: () => pedidoService.listar(verTodos),
    refetchInterval: 30_000,
  });

  const { data: productos = [] } = useQuery({
    queryKey: ["productos-activos"],
    queryFn: () => productoService.listar(true),
  });

  const { data: salones = [] } = useQuery({
    queryKey: ["salones"],
    queryFn: salonService.listar,
  });

  const invalidarPedidos = () => qc.invalidateQueries({ queryKey: ["pedidos"] });

  const crearMut = useMutation({
    mutationFn: pedidoService.crear,
    onSuccess: (res) => { invalidarPedidos(); setShowCrear(false); setPedidoCreadoExito(res); },
  });
  const agregarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.agregarDetalle(id, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const actualizarDetalleMut = useMutation({
    mutationFn: ({ id, detalleId, req }) => pedidoService.actualizarDetalle(id, detalleId, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const quitarMut = useMutation({
    mutationFn: ({ id, detalleId }) => pedidoService.quitarDetalle(id, detalleId),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const pagoMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.registrarPago(id, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const actualizarPagoMut = useMutation({
    mutationFn: ({ id, pagoId, req }) => pedidoService.actualizarPago(id, pagoId, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const finalizarMut = useMutation({
    mutationFn: (id) => pedidoService.finalizar(id),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const anularMut = useMutation({
    mutationFn: (id) => pedidoService.anular(id),
    onSuccess: () => { invalidarPedidos(); setPedidoActivo(null); },
  });
  const actualizarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.actualizar(id, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoActivo(res); },
  });
  const pagoCobrMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.registrarPago(id, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoCobrar(res); },
  });
  const finalizarCobrMut = useMutation({
    mutationFn: (id) => pedidoService.finalizar(id),
    onSuccess: (res) => { invalidarPedidos(); setPedidoCobrar(res); },
  });
  const actualizarPagoCobrMut = useMutation({
    mutationFn: ({ id, pagoId, req }) => pedidoService.actualizarPago(id, pagoId, req),
    onSuccess: (res) => { invalidarPedidos(); setPedidoCobrar(res); },
  });
  const quitarPagoCobrMut = useMutation({
    mutationFn: ({ id, pagoId }) => pedidoService.quitarPago(id, pagoId),
    onSuccess: (res) => { invalidarPedidos(); setPedidoCobrar(res); },
  });

  const abiertos   = pedidos.filter((p) => esActivo(p.estado));
  const pendientes = abiertos.filter((p) => (p.saldoPendiente ?? 0) > 0);
  const listos     = abiertos.filter((p) => (p.totalCop ?? 0) > 0 && (p.saldoPendiente ?? 0) <= 0);
  const sinItems   = abiertos.filter((p) => (p.totalCop ?? 0) === 0);
  const porCobrar  = pendientes.reduce((s, p) => s + (p.saldoPendiente ?? 0), 0);

  const historial       = pedidos.filter((p) => p.estado === "FINALIZADO" || p.estado === "ANULADO");
  const listaBase       = tab === "activos" ? abiertos : historial;
  const listaFiltrada   = busqueda
    ? listaBase.filter((p) => (p.nombre ?? "").toLowerCase().includes(busqueda.toLowerCase()))
    : listaBase;
  const pedidosMostrados = tab === "activos" ? sortByReserva(listaFiltrada) : listaFiltrada;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Pedidos</h1>
        <button className="btn btn-primary" onClick={() => setShowCrear(true)}>
          <Plus size={15} /> Nuevo pedido
        </button>
      </div>

      {tab === "activos" && (
        <div className="kpi-grid mb-5">
          <div className="kpi-card">
            <div className="kpi-label">Abiertos</div>
            <div className="kpi-value">{abiertos.length}</div>
          </div>
          <div className={`kpi-card ${porCobrar > 0 ? "border-l-red-600" : "border-l-verde-claro"}`}>
            <div className="kpi-label">Por cobrar</div>
            <div className={`kpi-value ${porCobrar > 0 ? "text-red-600" : ""}`}>{formatCOP(porCobrar)}</div>
            {pendientes.length > 0 && (
              <div className="kpi-label mt-0.5">{pendientes.length} con saldo pendiente</div>
            )}
          </div>
          <div className="kpi-card border-l-verde-claro">
            <div className="kpi-label">Listos para finalizar</div>
            <div className="kpi-value text-verde-claro">{listos.length}</div>
            <div className="kpi-label mt-0.5">pago completo</div>
          </div>
          {sinItems.length > 0 && (
            <div className="kpi-card border-l-gray-300">
              <div className="kpi-label">Sin ítems</div>
              <div className="kpi-value">{sinItems.length}</div>
            </div>
          )}
        </div>
      )}

      <div className="flex items-center justify-between mb-4">
        <div className="tabs">
          <button className={`tab${tab === "activos" ? " activo" : ""}`} onClick={() => setTab("activos")}>
            Activos
            {abiertos.length > 0 && (
              <span className="ml-1.5 bg-verde-claro text-white rounded-[10px] px-[7px] py-0.5 text-[11px] font-bold">
                {abiertos.length}
              </span>
            )}
          </button>
          <button className={`tab${tab === "historial" ? " activo" : ""}`} onClick={() => setTab("historial")}>
            Historial
          </button>
        </div>
        <div className="relative w-56">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
          <input
            className="form-control pl-8 pr-8 text-sm"
            placeholder="Buscar cliente…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          {busqueda && (
            <button
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              onClick={() => setBusqueda("")}
            >
              <X size={13} />
            </button>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : pedidosMostrados.length === 0 ? (
        <div className="empty-state">
          <p>
            {busqueda
              ? `Sin resultados para "${busqueda}"`
              : tab === "activos"
                ? "No hay pedidos abiertos. Crea el primero."
                : "No hay pedidos en el historial."
            }
          </p>
        </div>
      ) : (
        <div className="grid gap-[14px]" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))" }}>
          {pedidosMostrados.map((p, idx) => (
            <OrdenCard key={p.id} pedido={p} idx={idx}
              onEditar={(p) => setPedidoActivo(p)}
              onCobrar={(p) => setPedidoCobrar(p)}
            />
          ))}
        </div>
      )}

      {showCrear && (
        <ModalCrear
          salones={salones}
          productos={productos}
          onClose={() => setShowCrear(false)}
          onCrear={(req) => crearMut.mutateAsync(req)}
        />
      )}

      {pedidoActivo && (
        <ModalDetalle
          pedido={pedidoActivo}
          productos={productos}
          onClose={() => setPedidoActivo(null)}
          onActualizar={(req) => actualizarMut.mutateAsync({ id: pedidoActivo.id, req })}
          onAgregarDetalle={(req) => agregarMut.mutateAsync({ id: pedidoActivo.id, req })}
          onActualizarDetalle={(detalleId, req) => actualizarDetalleMut.mutateAsync({ id: pedidoActivo.id, detalleId, req })}
          onQuitarDetalle={(detalleId) => quitarMut.mutateAsync({ id: pedidoActivo.id, detalleId })}
          onFinalizar={() => finalizarMut.mutateAsync(pedidoActivo.id)}
          onAnular={() => anularMut.mutateAsync(pedidoActivo.id)}
        />
      )}

      {pedidoCobrar && (
        <ModalCobrar
          pedido={pedidoCobrar}
          onClose={() => setPedidoCobrar(null)}
          onPago={(req) => pagoCobrMut.mutateAsync({ id: pedidoCobrar.id, req })}
          onActualizarPago={(pagoId, req) => actualizarPagoCobrMut.mutateAsync({ id: pedidoCobrar.id, pagoId, req })}
          onQuitarPago={(pagoId) => quitarPagoCobrMut.mutateAsync({ id: pedidoCobrar.id, pagoId })}
          onFinalizar={() => finalizarCobrMut.mutateAsync(pedidoCobrar.id)}
        />
      )}

      {pedidoCreadoExito && (
        <PedidoCreado
          pedido={pedidoCreadoExito}
          onVerPedido={() => { setPedidoActivo(pedidoCreadoExito); setPedidoCreadoExito(null); }}
          onNuevoPedido={() => { setPedidoCreadoExito(null); setShowCrear(true); }}
          onCerrar={() => setPedidoCreadoExito(null)}
        />
      )}
    </div>
  );
}
