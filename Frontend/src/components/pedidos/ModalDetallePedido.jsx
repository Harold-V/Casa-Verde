import { useState, useRef, useEffect } from "react";
import { Ban, CheckCircle, Pencil, Plus, Trash2, Utensils, X } from "lucide-react";
import { formatCOP, formatFecha } from "../../utils/format";
import { ESTADO_BADGE } from "../../utils/constants";
import {
  calcularInsumosDetallado, formatInsumo, esActivo, esEnCursoPedido,
  normalizarFracciones, toTitleCase, formatReservaDisplay,
} from "../../utils/pedidoHelpers";
import MoneyInput from "../shared/MoneyInput";
import ToggleChip from "../shared/ToggleChip";

import ModalAgregarItems from "./ModalAgregarItems";
import ConfirmDialog from "../shared/ConfirmDialog";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/**
 * Modal de detalle y edición de un pedido activo.
 * Gestiona ítems, datos generales, finalización y anulación.
 */
export default function ModalDetallePedido({
  pedido, productos, salones = [], onClose,
  onActualizar,
  onAgregarDetalle, onActualizarDetalle, onQuitarDetalle,
  onFinalizar, onAnular,
  onActualizarPago, onQuitarPago,
}) {
  useModalKeyboard({ onEscape: onClose });

  const [showAgregarOverlay, setShowAgregarOverlay] = useState(false);
  const [editandoDetalleId, setEditandoDetalleId]   = useState(null);
  const [editCantidad, setEditCantidad]             = useState("");
  const [loading, setLoading]                       = useState(false);
  const [error, setError]                           = useState("");
  const [removingIds, setRemovingIds]               = useState(new Set());
  const [confirm, setConfirm]                       = useState(null);
  // Edición nombre
  const [editandoNombre, setEditandoNombre]         = useState(false);
  const [editNombre, setEditNombre]                 = useState(pedido.nombre ?? "");
  // Edición de datos generales
  const [showEditReserva, setShowEditReserva]       = useState(false);
  const [editOn, setEditOn]                         = useState({ hora: false, fecha: false, personas: false, salon: false });
  const [editHora, setEditHora]                     = useState("");
  const [editFecha, setEditFecha]                   = useState("");
  const [editPersonas, setEditPersonas]             = useState("");
  const [editSalonId, setEditSalonId]               = useState("");
  const [editParaLlevar, setEditParaLlevar]         = useState(!!pedido.paraLlevar);
  const [editObservacion, setEditObservacion]       = useState(pedido.observacion ?? "");
  // Cobros
  const [editandoPagoId, setEditandoPagoId] = useState(null);
  const [editPagoForm, setEditPagoForm]     = useState({ moneda: "COP", monto: "" });

  // Sincroniza con los valores actuales del pedido al abrir el panel
  useEffect(() => {
    if (showEditReserva) {
      setEditOn({
        hora:     !!pedido.horaReserva,
        fecha:    !!pedido.fechaReserva,
        personas: !!pedido.cantPersonas,
        salon:    !!pedido.salonId,
      });
      setEditHora(pedido.horaReserva  ?? new Date().toTimeString().slice(0, 5));
      setEditFecha(pedido.fechaReserva ?? new Date().toLocaleDateString("en-CA"));
      setEditPersonas(pedido.cantPersonas ? String(pedido.cantPersonas) : "");
      setEditSalonId(pedido.salonId    ? String(pedido.salonId)    : "");
      setEditParaLlevar(!!pedido.paraLlevar);
      setEditObservacion(pedido.observacion ?? "");
    }
  }, [showEditReserva]); // eslint-disable-line react-hooks/exhaustive-deps

  const togEdit = (k) => setEditOn((o) => {
    const val   = !o[k];
    const nuevo = { ...o, [k]: val };
    if (k === "fecha" && val && !o.hora) nuevo.hora = true;
    if (k === "hora"  && !val)           nuevo.fecha = false;
    return nuevo;
  });

  const abierto       = esActivo(pedido.estado);
  const reservaFutura = !!(pedido.fechaReserva || pedido.horaReserva) && !esEnCursoPedido(pedido);
  const { totales: itotales, almuerzo: ialmuerzo } = calcularInsumosDetallado(pedido.detalles);

  const buildUpdateReq = (overrides) => ({
    nombre:       pedido.nombre       ?? null,
    fechaReserva: pedido.fechaReserva ?? null,
    horaReserva:  pedido.horaReserva  ?? null,
    mesa:         pedido.mesa         ?? null,
    cantPersonas: pedido.cantPersonas ?? null,
    observacion:  pedido.observacion  ?? null,
    salonId:      pedido.salonId      ?? null,
    paraLlevar:   pedido.paraLlevar   ?? null,
    ...overrides,
  });

  // normalizandoRef: true mientras se ejecuta una normalización → evita falso positivo
  const prevItemsRef    = useRef(pedido.detalles.length);
  const normalizandoRef = useRef(false);
  useEffect(() => {
    const prev = prevItemsRef.current;
    prevItemsRef.current = pedido.detalles.length;
    if (abierto && prev > 0 && pedido.detalles.length === 0 && !normalizandoRef.current) {
      setConfirm({ tipo: "anular-sin-items" });
    }
  }, [pedido.detalles.length, abierto]);

  const run = async (fn) => {
    setLoading(true); setError("");
    try { await fn(); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  };

  const handleGuardarNombre = () => run(async () => {
    if (!editNombre.trim()) throw new Error("El nombre del cliente es obligatorio");
    await onActualizar(buildUpdateReq({ nombre: editNombre.trim() }));
    setEditandoNombre(false);
  });

  const handleGuardarDetalle = (detalleId) => run(async () => {
    const cant = Number(editCantidad);
    if (isNaN(cant) || cant < 0) throw new Error("Cantidad inválida");

    const detalleEditado = pedido.detalles.find((d) => d.id === detalleId);

    // Ítems manuales: actualizar directo, sin normalización
    if (!detalleEditado?.productoId) {
      if (cant === 0) await onQuitarDetalle(detalleId);
      else            await onActualizarDetalle(detalleId, { cantidad: cant });
      setEditandoDetalleId(null);
      return;
    }

    const conProducto = pedido.detalles.filter((d) => d.productoId);
    const antes = Object.fromEntries(conProducto.map((d) => [d.productoId, d.cantidad]));
    const nuevas = { ...antes, [detalleEditado.productoId]: cant };
    if (cant === 0) delete nuevas[detalleEditado.productoId];

    const normalizadas = normalizarFracciones(
      Object.fromEntries(Object.entries(nuevas).map(([k, v]) => [Number(k), v])),
      productos,
    );

    const todasKeys = new Set([
      ...Object.keys(antes).map(Number),
      ...Object.keys(normalizadas).map(Number),
    ]);
    const allDiffs = [...todasKeys]
      .map((pid) => ({ pid, a: antes[pid] ?? 0, d: normalizadas[pid] ?? 0 }))
      .filter(({ a, d }) => a !== d);

    if (allDiffs.length > 0) {
      normalizandoRef.current = true;
      try {
        for (const { pid, d } of allDiffs.filter(({ a }) => a === 0))
          await onAgregarDetalle({ productoId: pid, cantidad: d });
        for (const { pid, d } of allDiffs.filter(({ a, d }) => a > 0 && d > 0)) {
          const det = conProducto.find((x) => x.productoId === pid);
          if (det) await onActualizarDetalle(det.id, { cantidad: d });
        }
        const idsQuitar = allDiffs
          .filter(({ d }) => d === 0)
          .map(({ pid }) => conProducto.find((x) => x.productoId === pid)?.id)
          .filter(Boolean);
        if (idsQuitar.length > 0) {
          setRemovingIds(new Set(idsQuitar));
          await new Promise((r) => setTimeout(r, 190));
        }
        for (const { pid } of allDiffs.filter(({ d }) => d === 0)) {
          const det = conProducto.find((x) => x.productoId === pid);
          if (det) await onQuitarDetalle(det.id);
        }
      } finally {
        normalizandoRef.current = false;
        setRemovingIds(new Set());
      }
    }

    setEditandoDetalleId(null);
  });

  const handleGuardarReserva = () => run(async () => {
    await onActualizar(buildUpdateReq({
      horaReserva:  editOn.hora     ? editHora                    : null,
      fechaReserva: editOn.fecha    ? editFecha                   : null,
      cantPersonas: editOn.personas ? (Number(editPersonas) || null) : null,
      salonId:      editOn.salon    ? (Number(editSalonId)  || null) : null,
      paraLlevar:   editParaLlevar,
      observacion:  editObservacion.trim() || null,
    }));
    setShowEditReserva(false);
  });

  const handleGuardarPago = (pagoId) => run(async () => {
    const monto = Number(editPagoForm.monto);
    if (isNaN(monto) || monto < 0) throw new Error("Monto inválido");
    if (monto === 0) await onQuitarPago(pagoId);
    else await onActualizarPago(pagoId, { moneda: editPagoForm.moneda, monto });
    setEditandoPagoId(null);
  });

  const handleClose = () => {
    if (abierto && pedido.detalles.length === 0) {
      setConfirm({ tipo: "anular-sin-items" });
    } else {
      onClose();
    }
  };

  const handleConfirmarAgregarItems = async ({ diffs, manuales }) => {
    setLoading(true); setError("");
    try {
      if (diffs.length > 0) {
        normalizandoRef.current = true;
        for (const diff of diffs.filter((d) => d.cantidadAntes === 0))
          await onAgregarDetalle({ productoId: diff.productoId, cantidad: diff.cantidadDespues });
        for (const diff of diffs.filter((d) => d.cantidadAntes > 0 && d.cantidadDespues > 0)) {
          const det = pedido.detalles.find((d) => d.productoId === diff.productoId);
          if (det) await onActualizarDetalle(det.id, { cantidad: diff.cantidadDespues });
        }
        const idsQuitar = diffs
          .filter((d) => d.cantidadDespues === 0)
          .map((d) => pedido.detalles.find((det) => det.productoId === d.productoId)?.id)
          .filter(Boolean);
        if (idsQuitar.length > 0) {
          setRemovingIds(new Set(idsQuitar));
          await new Promise((r) => setTimeout(r, 190));
        }
        for (const diff of diffs.filter((d) => d.cantidadDespues === 0)) {
          const det = pedido.detalles.find((d) => d.productoId === diff.productoId);
          if (det) await onQuitarDetalle(det.id);
        }
        normalizandoRef.current = false;
      }
      for (const m of manuales) await onAgregarDetalle(m);
    } catch (err) {
      normalizandoRef.current = false;
      setRemovingIds(new Set());
      setError(err.message);
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
              {editandoNombre ? (
                <div className="flex items-center gap-2 mb-1">
                  <input
                    className="form-control text-[15px] font-semibold w-60"
                    value={editNombre}
                    onChange={(e) => setEditNombre(toTitleCase(e.target.value))}
                    onKeyDown={(e) => { if (e.key === "Enter") handleGuardarNombre(); if (e.key === "Escape") setEditandoNombre(false); }}
                    autoFocus
                  />
                  <button className="btn btn-primary btn-sm" onClick={handleGuardarNombre} disabled={loading}>Guardar</button>
                  <button className="btn-icon text-gray-400" onClick={() => setEditandoNombre(false)}><X size={14} /></button>
                </div>
              ) : (
                <div className="flex items-center gap-2.5 flex-wrap mb-0.5">
                  <span className="font-extrabold text-xl text-verde-claro">
                    {pedido.nombre ?? `Pedido #${pedido.id}`}
                  </span>
                  {abierto && (
                    <button className="btn-icon text-gray-400 hover:text-verde-oscuro -ml-1" title="Editar nombre"
                      onClick={() => { setEditandoNombre(true); setEditNombre(pedido.nombre ?? ""); }}>
                      <Pencil size={13} />
                    </button>
                  )}
                  <span className={`badge ${ESTADO_BADGE[pedido.estado] ?? "badge-gray"}`}>{pedido.estado}</span>
                  {pedido.paraLlevar && (
                    <button
                      className="flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 border border-blue-300 hover:bg-blue-200 transition-colors"
                      onClick={() => abierto && setShowEditReserva((v) => !v)}
                    >
                      PARA LLEVAR {abierto && <Pencil size={8} />}
                    </button>
                  )}
                </div>
              )}
              <div className="text-[12px] text-gray-500 flex gap-2.5 mt-0.5 flex-wrap">
                <span>#{pedido.id}</span>
                {pedido.salonNombre && <span>· {pedido.salonNombre}{pedido.mesa ? ` / ${pedido.mesa}` : ""}</span>}
                {pedido.cantPersonas && <span>· {pedido.cantPersonas} pers.</span>}
                {(pedido.fechaReserva || pedido.horaReserva) && (
                  <span className="flex items-center gap-1">
                    · 📅 {formatReservaDisplay(pedido.fechaReserva, pedido.horaReserva)}
                    {abierto && (
                      <button className="btn-icon p-0 text-gray-400 hover:text-verde-oscuro" onClick={() => setShowEditReserva((v) => !v)}>
                        <Pencil size={10} />
                      </button>
                    )}
                  </span>
                )}
                {pedido.observacion && (
                  <span className="flex items-center gap-1 max-w-[240px]">
                    · <span className="italic truncate">📝 {pedido.observacion}</span>
                    {abierto && (
                      <button className="btn-icon p-0 text-gray-400 hover:text-verde-oscuro shrink-0" onClick={() => setShowEditReserva((v) => !v)}>
                        <Pencil size={10} />
                      </button>
                    )}
                  </span>
                )}
                {abierto && (
                  <button
                    className="text-gray-400 hover:text-verde-oscuro text-[11px] underline underline-offset-2"
                    onClick={() => setShowEditReserva((v) => !v)}
                  >
                    ✏️ Editar opciones
                  </button>
                )}
              </div>
            </div>
            <button className="btn-icon" onClick={handleClose}><X size={20} /></button>
          </div>

          {/* Panel edición general */}
          {showEditReserva && abierto && (
            <div className="px-6 py-3 border-b border-gray-100 bg-gray-50/60 flex flex-col gap-2.5 shrink-0 animate-slide-down">
              <div className="flex flex-wrap gap-1.5">
                <ToggleChip label="🕐 Reserva"     active={editOn.hora}     onToggle={() => togEdit("hora")} />
                <ToggleChip label="👥 Personas"    active={editOn.personas} onToggle={() => togEdit("personas")} />
                <ToggleChip label="🪑 Salón"       active={editOn.salon}    onToggle={() => togEdit("salon")} />
                <ToggleChip label="🛵 Para llevar" active={editParaLlevar}  onToggle={() => setEditParaLlevar((v) => !v)} />
              </div>
              {(editOn.hora || editOn.personas || editOn.salon) && (
                <div className="flex flex-wrap gap-2 items-center">
                  {editOn.hora && (
                    <>
                      <input type="time" className="form-control min-w-[110px] text-sm"
                        value={editHora} onChange={(e) => setEditHora(e.target.value)} />
                      <ToggleChip label="📅 + Fecha" active={editOn.fecha} onToggle={() => togEdit("fecha")} />
                      {editOn.fecha && (
                        <input type="date" className="form-control text-sm flex-1 min-w-[140px]"
                          value={editFecha} onChange={(e) => setEditFecha(e.target.value)} />
                      )}
                    </>
                  )}
                  {editOn.personas && (
                    <input type="number" min="1" placeholder="Personas"
                      className="form-control w-28 text-sm"
                      value={editPersonas} onChange={(e) => setEditPersonas(e.target.value)} />
                  )}
                  {editOn.salon && (
                    <select className="form-control flex-1 text-sm"
                      value={editSalonId} onChange={(e) => setEditSalonId(e.target.value)}>
                      <option value="">Salón…</option>
                      {salones.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
                    </select>
                  )}
                </div>
              )}
              <textarea
                className="form-control text-sm resize-none"
                rows={2}
                placeholder="Observación del pedido (opcional)…"
                value={editObservacion}
                onChange={(e) => setEditObservacion(e.target.value)}
              />
              <div className="flex gap-2">
                <button className="btn btn-primary btn-sm" onClick={handleGuardarReserva} disabled={loading}>Guardar</button>
                <button className="btn-icon text-gray-400" onClick={() => setShowEditReserva(false)}><X size={14} /></button>
              </div>
            </div>
          )}

          {/* Body */}
          <div className="flex flex-col flex-1 overflow-hidden">

            <div className="flex items-center justify-between px-5 py-3 border-b border-gray-100 shrink-0">
              <span className="font-bold text-sm text-gray-600">Ítems · {pedido.detalles.length}</span>
              {abierto && (
                <button className="btn btn-secondary btn-sm" onClick={() => { setShowAgregarOverlay(true); setError(""); }}>
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
                        🐹 {formatInsumo("cuy", itotales.cuy)}
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

            {/* Lista de ítems */}
            <div className="flex-1 overflow-y-auto px-5 py-3">
              {error && <div className="alert alert-error mb-3">{error}</div>}
              {pedido.detalles.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                  <Utensils size={44} className="mb-3 opacity-25" />
                  <p className="text-sm font-medium">Sin ítems</p>
                  {abierto && <p className="text-xs mt-1">Usa "Agregar" para añadir productos</p>}
                </div>
              ) : (
                <div className={`flex flex-col gap-1.5 transition-opacity duration-300 ${loading ? "opacity-40 pointer-events-none" : "opacity-100"}`}>
                  {pedido.detalles.map((d, i) => (
                    editandoDetalleId === d.id ? (
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
                      <div key={d.id}
                        className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg border border-gray-200 ${removingIds.has(d.id) ? "animate-fade-out-card pointer-events-none" : "animate-fade-in-card"} ${i % 2 === 0 ? "bg-gray-50" : "bg-white"}`}>
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

            {/* ── Cobros (solo editar / eliminar) ── */}
            {(pedido.pagos?.length ?? 0) > 0 && (
              <div className="px-5 py-3 border-t border-gray-100 shrink-0">
                <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">
                  Cobros · {pedido.pagos.length}
                </div>
                <div className="flex flex-col gap-1.5">
                  {pedido.pagos.map((p) => {
                    const editando = editandoPagoId === p.id;
                    return editando ? (
                      <div key={p.id} className="px-3 py-2 rounded-lg bg-verde-bg border border-verde-claro/30 animate-slide-down">
                        <div className="flex gap-2">
                          <select className="form-control text-sm w-24" value={editPagoForm.moneda}
                            onChange={(e) => setEditPagoForm((f) => ({ ...f, moneda: e.target.value }))}>
                            <option value="COP">COP</option>
                            <option value="USD">USD</option>
                            <option value="NEQUI">Nequi</option>
                          </select>
                          <MoneyInput className="form-control flex-1 text-sm" moneda={editPagoForm.moneda}
                            placeholder={editPagoForm.moneda === "USD" ? "Dólares" : "Pesos"}
                            value={editPagoForm.monto}
                            onChange={(v) => setEditPagoForm((f) => ({ ...f, monto: v }))}
                            autoFocus />
                          <button type="button" className="btn btn-primary btn-sm"
                            onClick={() => handleGuardarPago(p.id)} disabled={loading}>Guardar</button>
                          <button type="button" className="btn-icon text-red-500 shrink-0"
                            title="Eliminar cobro" disabled={loading}
                            onClick={() => run(async () => { await onQuitarPago(p.id); setEditandoPagoId(null); })}>
                            <Trash2 size={14} />
                          </button>
                          <button type="button" className="btn-icon text-gray-400"
                            onClick={() => setEditandoPagoId(null)}><X size={14} /></button>
                        </div>
                      </div>
                    ) : (
                      <div key={p.id} className="flex items-center gap-2 text-[13px]">
                        <span className={`badge shrink-0 ${p.moneda === "USD" ? "badge-warning" : p.moneda.startsWith("NEQUI") ? "badge-gray" : "badge-success"}`}>
                          {p.moneda}
                        </span>
                        <span className="flex-1 font-semibold">{formatCOP(p.montoCop)}</span>
                        <span className="text-[11px] text-gray-400">{formatFecha(p.registradoEn)}</span>
                        {abierto && (
                          <button className="btn-icon text-gray-400 hover:text-verde-oscuro"
                            onClick={() => { setEditandoPagoId(p.id); setEditPagoForm({ moneda: p.moneda, monto: String(p.monto) }); }}>
                            <Pencil size={12} />
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Acciones */}
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
                  <button
                    type="button"
                    className="btn btn-primary bg-verde-oscuro hover:bg-[#0f2b1e] disabled:opacity-40 disabled:cursor-not-allowed"
                    onClick={() => !reservaFutura && setConfirm({ tipo: "finalizar" })}
                    disabled={loading || reservaFutura}
                    title={reservaFutura ? `Disponible al llegar la reserva: ${formatReservaDisplay(pedido.fechaReserva, pedido.horaReserva)}` : undefined}
                  >
                    <CheckCircle size={15} /> Finalizar
                  </button>
                )}
                <button type="button" className="btn btn-secondary" onClick={handleClose}>Cerrar</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {showAgregarOverlay && (
        <ModalAgregarItems
          productos={productos}
          detalles={pedido.detalles}
          onConfirmar={handleConfirmarAgregarItems}
          onCancelar={() => setShowAgregarOverlay(false)}
        />
      )}

      {confirm && (
        <ConfirmDialog
          emoji={confirm.tipo === "anular" ? "🚫" : confirm.tipo === "anular-sin-items" ? "⚠️" : "✅"}
          titulo={
            confirm.tipo === "anular"           ? "¿Anular el pedido?"
            : confirm.tipo === "anular-sin-items" ? "Pedido sin ítems"
            : "¿Finalizar el pedido?"
          }
          descripcion={
            confirm.tipo === "anular"
              ? "Esta acción no se puede deshacer."
              : confirm.tipo === "anular-sin-items"
              ? "Quitaste el último ítem. ¿Deseas anular el pedido? Puedes cancelar y agregar ítems primero."
              : "Se descontará el inventario correspondiente al cerrar el pedido."
          }
          labelOk={confirm.tipo !== "finalizar" ? "Sí, anular" : "Sí, finalizar"}
          peligro={confirm.tipo !== "finalizar"}
          onOk={() => {
            run(confirm.tipo === "finalizar" ? onFinalizar : onAnular);
            setConfirm(null);
          }}
          onCancelar={() => setConfirm(null)}
        />
      )}
    </>
  );
}
