import { useState } from "react";
import { Plus, Search, Utensils, Wallet, X } from "lucide-react";
import { formatCOP } from "../../utils/format";
import { normalizarFracciones, toTitleCase } from "../../utils/pedidoHelpers";
import MoneyInput from "../shared/MoneyInput";
import ToggleChip from "../shared/ToggleChip";
import ProductoRow from "./ProductoRow";
import ConfirmDialog from "../shared/ConfirmDialog";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/** Modal de creación de pedido: datos del cliente + catálogo + abono inicial. */
export default function ModalCrearPedido({ salones, productos, onClose, onCrear }) {
  const [nombre, setNombre]         = useState("");
  const [on, setOn]                 = useState({ fecha: false, hora: false, personas: false, salon: false });
  const [fecha, setFecha]           = useState(new Date().toLocaleDateString("en-CA"));
  const [hora, setHora]             = useState(new Date().toTimeString().slice(0, 5));
  const [personas, setPersonas]     = useState(1);
  const [salonId, setSalonId]       = useState("");
  const [paraLlevar, setParaLlevar] = useState(false);
  const [observacion, setObservacion] = useState("");
  const [cantidades, setCantidades] = useState({});
  const [manual, setManual]         = useState({ nombre: "", precio: "" });
  const [manualItems, setManualItems] = useState([]);
  const [busqueda, setBusqueda]     = useState("");
  const [showAbono, setShowAbono]   = useState(false);
  const [abonoMoneda, setAbonoMoneda] = useState("COP");
  const [abonoMonto, setAbonoMonto] = useState("");
  const [confirmCancelar, setConfirmCancelar] = useState(false);
  const [error, setError]           = useState("");
  const [saving, setSaving]         = useState(false);

  const tog = (k) => setOn((o) => {
    const next = { ...o, [k]: !o[k] };
    if (k === "fecha" && next.fecha) next.hora = true;
    if (k === "hora"  && !next.hora) next.fecha = false;
    return next;
  });

  const cambiarCantidad = (id, n) =>
    setCantidades((c) => {
      const updated = n <= 0
        ? (() => { const { [id]: _, ...rest } = c; return rest; })()
        : { ...c, [id]: n };
      return normalizarFracciones(updated, productos);
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
        fechaReserva: on.fecha ? fecha : (on.hora ? new Date().toLocaleDateString("en-CA") : null),
        horaReserva:  on.hora    ? hora    : null,
        cantPersonas: on.personas ? Number(personas) : null,
        paraLlevar:   paraLlevar || null,
        observacion:  observacion.trim() || null,
        abono:        showAbono && abonoMonto ? parseFloat(abonoMonto) : null,
        monedaAbono:  showAbono && abonoMonto ? abonoMoneda : null,
        detalles,
      });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  // Esc abre confirm cancelar; Enter crea el pedido (salvo que esté guardando o focus esté en form de ítems manuales)
  useModalKeyboard({ onEscape: () => setConfirmCancelar(true), onEnter: saving ? undefined : handleCrear });

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
                  onChange={(e) => setNombre(toTitleCase(e.target.value))}
                  autoFocus
                />
                <div className="flex flex-wrap gap-1.5">
                  <ToggleChip label="🕐 Reserva"    active={on.hora}     onToggle={() => tog("hora")} />
                  <ToggleChip label="👥 Personas"   active={on.personas} onToggle={() => tog("personas")} />
                  <ToggleChip label="🪑 Salón"      active={on.salon}    onToggle={() => tog("salon")} />
                  <ToggleChip label="🛵 Para llevar" active={paraLlevar}  onToggle={() => setParaLlevar((v) => !v)} />
                </div>
                <textarea
                  className="form-control text-sm resize-none"
                  placeholder="Observación del pedido (opcional)"
                  rows={2}
                  value={observacion}
                  onChange={(e) => setObservacion(e.target.value)}
                />
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
                  <form onSubmit={(e) => { e.preventDefault(); agregarManual(); }} className="flex gap-2 items-center">
                    <input
                      className="form-control flex-1 text-sm"
                      placeholder="Descripción"
                      value={manual.nombre}
                      onChange={(e) => setManual((m) => ({ ...m, nombre: e.target.value }))}
                    />
                    <input
                      className="form-control w-28 text-sm"
                      type="number" min="0" placeholder="Precio COP"
                      value={manual.precio}
                      onChange={(e) => setManual((m) => ({ ...m, precio: e.target.value }))}
                    />
                    <button type="submit" className="btn btn-secondary btn-sm shrink-0" title="Agregar ítem">
                      <Plus size={14} />
                    </button>
                  </form>
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
