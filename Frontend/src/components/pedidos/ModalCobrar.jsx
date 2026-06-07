import { useState } from "react";
import { Calendar, CheckCircle, CreditCard, X } from "lucide-react";
import { formatCOP, formatFecha } from "../../utils/format";
import { esActivo, formatReservaDisplay } from "../../utils/pedidoHelpers";
import MoneyInput from "../shared/MoneyInput";
import ConfirmDialog from "../shared/ConfirmDialog";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/** Modal de cobro rápido: factura + registro de pagos (solo lectura) + finalizar. */
export default function ModalCobrar({ pedido, onClose, onPago, onFinalizar }) {
  const [pagoForm, setPagoForm]         = useState({ moneda: "COP", monto: "" });
  const [showPagoForm, setShowPagoForm] = useState(false);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState("");
  const [confirm, setConfirm]           = useState(false);
  const abierto       = esActivo(pedido.estado);
  const reservaFutura = !!(pedido.fechaReserva || pedido.horaReserva) && !pedido.enCurso;

  const run = async (fn) => {
    setLoading(true); setError("");
    try { await fn(); }
    catch (e) { setError(e.message); }
    finally { setLoading(false); }
  };

  const handlePago = () => run(async () => {
    if (!pagoForm.monto || Number(pagoForm.monto) <= 0) throw new Error("El monto debe ser mayor a 0");
    await onPago({ moneda: pagoForm.moneda, monto: Number(pagoForm.monto) });
    setPagoForm({ moneda: "COP", monto: "" }); setShowPagoForm(false);
  });

  // Esc cierra. Enter: si el form de pago está abierto → registrar cobro,
  // si saldo = 0 → confirmar finalizar, si saldo > 0 → abrir form de cobro.
  useModalKeyboard({
    onEscape: onClose,
    onEnter: showPagoForm
      ? handlePago
      : abierto && (pedido.saldoPendiente ?? 0) <= 0 && (pedido.detalles?.length ?? 0) > 0 && !reservaFutura
        ? () => setConfirm(true)
        : abierto && (pedido.saldoPendiente ?? 0) > 0
          ? () => { setShowPagoForm(true); setError(""); }
          : undefined,
  });

  return (
    <>
      <div className="fixed inset-0 bg-black/45 z-[1000] flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md flex flex-col overflow-hidden animate-slide-up" style={{ maxHeight: "92vh" }}>

          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="font-extrabold text-xl text-verde-claro">{pedido.nombre}</h2>
                {pedido.paraLlevar && (
                  <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 border border-blue-300">
                    PARA LLEVAR
                  </span>
                )}
              </div>
              <div className="text-[12px] text-gray-500 mt-0.5 flex gap-2 flex-wrap">
                <span>#{pedido.id}</span>
                {pedido.salonNombre && <span>· {pedido.salonNombre}</span>}
                {(pedido.fechaReserva || pedido.horaReserva) && (
                  <span>· 📅 {formatReservaDisplay(pedido.fechaReserva, pedido.horaReserva)}</span>
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
              {(pedido.valorCambio ?? 0) > 0 ? (
                <div className="flex justify-between py-1.5 font-bold text-[16px] text-amber-600 border-t border-amber-100 mt-1 pt-1">
                  <span>💵 Cambio</span><span>{formatCOP(pedido.valorCambio)}</span>
                </div>
              ) : (
                <div className={`flex justify-between py-1.5 font-bold text-[16px] ${(pedido.saldoPendiente ?? 0) > 0 ? "text-red-600" : "text-verde-claro"}`}>
                  <span>Saldo</span><span>{formatCOP(Math.max(0, pedido.saldoPendiente ?? 0))}</span>
                </div>
              )}
            </div>

            {pedido.pagos.length > 0 && (
              <div className="px-6 py-3 border-b border-gray-200">
                <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Cobros · {pedido.pagos.length}</div>
                <div className="flex flex-col gap-1.5">
                  {pedido.pagos.map((p) => (
                    <div key={p.id} className="flex items-center gap-2 text-[13px]">
                      <span className={`badge shrink-0 ${p.moneda === "USD" ? "badge-warning" : p.moneda.startsWith("NEQUI") ? "badge-gray" : "badge-success"}`}>{p.moneda}</span>
                      <span className="flex-1 font-semibold">{formatCOP(p.montoCop)}</span>
                      <span className="text-[11px] text-gray-400">{formatFecha(p.registradoEn)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {abierto && (
              <div className="px-6 py-3">
                {error && <div className="alert alert-error mb-2 text-sm">{error}</div>}
                {!showPagoForm ? (
                  pedido.saldoPendiente <= 0 ? (
                    <div className="flex flex-col gap-2">
                      {reservaFutura && (
                        <div className="flex items-start gap-2 text-[12px] text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
                          <Calendar size={13} className="shrink-0 mt-0.5" />
                          <span>
                            Habilitado al llegar la reserva:{" "}
                            <strong>{formatReservaDisplay(pedido.fechaReserva, pedido.horaReserva)}</strong>
                          </span>
                        </div>
                      )}
                      <button
                        className="btn btn-primary w-full justify-center bg-verde-oscuro hover:bg-[#0f2b1e] disabled:opacity-40 disabled:cursor-not-allowed"
                        onClick={() => setConfirm(true)}
                        disabled={loading || pedido.detalles.length === 0 || reservaFutura}
                      >
                        <CheckCircle size={14} /> Finalizar pedido
                      </button>
                    </div>
                  ) : (
                    <button className="btn btn-secondary w-full justify-center" onClick={() => { setShowPagoForm(true); setError(""); }}>
                      <CreditCard size={14} /> Registrar cobro
                    </button>
                  )
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

          <div className="flex items-center gap-2 px-6 py-4 border-t border-gray-200 bg-white shrink-0 justify-end">
            <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
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
