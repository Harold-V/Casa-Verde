import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Edit2, X, TrendingUp, TrendingDown, DollarSign } from "lucide-react";
import { fondoService } from "../../services/fondoService";
import { tasaService } from "../../services/tasaService";
import { formatCOP } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

const MONEDA_LABEL = { COP: "Pesos colombianos", USD: "Dólares estadounidenses", NEQUI: "Nequi" };
const MONEDA_COLOR_CLS = {
  COP:   "text-verde-claro",
  USD:   "text-[#1a6b3c]",
  NEQUI: "text-[#8c00ff]",
};
const MONEDA_BORDER_CLS = {
  COP:   "border-l-verde-claro",
  USD:   "border-l-[#1a6b3c]",
  NEQUI: "border-l-[#8c00ff]",
};

function ModalAjuste({ fondo, onClose, onAjustar }) {
  const [nuevoSaldo, setNuevoSaldo] = useState(fondo.saldo ?? 0);
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (nuevoSaldo === "" || isNaN(Number(nuevoSaldo))) {
      setError("Ingresa un saldo válido");
      return;
    }
    setSaving(true);
    try {
      await onAjustar(fondo.moneda, Number(nuevoSaldo), motivo);
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message);
      setSaving(false);
    }
  };

  const diferencia = Number(nuevoSaldo) - (fondo.saldo ?? 0);

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[420px]">
        <div className="modal-header">
          <span className="modal-title">Ajustar fondo {fondo.moneda}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-group">
              <label className="form-label">
                Saldo actual: <strong>
                  {fondo.moneda === "USD"
                    ? `$${(fondo.saldo ?? 0).toFixed(2)} USD`
                    : formatCOP(fondo.saldo ?? 0)}
                </strong>
              </label>
              <input
                className="form-control"
                type="number"
                step={fondo.moneda === "USD" ? "0.01" : "1"}
                value={nuevoSaldo}
                onChange={(e) => setNuevoSaldo(e.target.value)}
                autoFocus
              />
              {diferencia !== 0 && (
                <div className={`mt-1.5 text-[13px] ${diferencia > 0 ? "text-verde-claro" : "text-red-600"}`}>
                  {diferencia > 0 ? "+" : ""}{fondo.moneda === "USD"
                    ? `$${diferencia.toFixed(2)} USD`
                    : formatCOP(diferencia)}
                </div>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Motivo (opcional)</label>
              <input
                className="form-control"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="ej. Capital inicial, corrección de caja"
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Aplicar ajuste"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function FondosPage() {
  const qc = useQueryClient();
  const { formatFecha, formatHora } = useFormatos();
  const [ajustando, setAjustando] = useState(null);

  const { data: fondos = [], isLoading: loadingFondos } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const { data: historial = [], isLoading: loadingHistorial } = useQuery({
    queryKey: ["fondos-historial"],
    queryFn: fondoService.historial,
  });

  const { data: tasa } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
    retry: false,
  });

  const ajustarMut = useMutation({
    mutationFn: ({ moneda, nuevoSaldo, motivo }) =>
      fondoService.ajustar(moneda, nuevoSaldo, motivo),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["fondos"] }),
  });

  const saldoCop   = fondos.find((f) => f.moneda === "COP")?.saldo   ?? 0;
  const saldoUsd   = fondos.find((f) => f.moneda === "USD")?.saldo   ?? 0;
  const saldoNequi = fondos.find((f) => f.moneda === "NEQUI")?.saldo ?? 0;
  const tasaValor  = tasa?.valorCop ?? null;
  const usdEnCop   = tasaValor != null ? saldoUsd * tasaValor : null;
  const capitalTotal = tasaValor != null
    ? saldoCop + saldoNequi + usdEnCop
    : saldoCop + saldoNequi;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Fondos</h1>
      </div>

      {/* Tarjeta capital neto */}
      {!loadingFondos && (
        <div className="card mb-5 border-l-4 border-l-verde-oscuro"
          style={{ background: "linear-gradient(135deg, #1a4731 0%, #1a4a2e 100%)" }}>
          <div className="p-[18px] flex justify-between items-center flex-wrap gap-3">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <DollarSign size={18} className="text-white/70" />
                <span className="text-[13px] text-white/75 font-medium">Capital neto acumulado</span>
              </div>
              <div className="text-[34px] font-black tracking-tight text-white">
                {formatCOP(capitalTotal)}
              </div>
              <div className="text-[11px] text-white/55 mt-1">
                COP {formatCOP(saldoCop)}
                {" + "}
                NEQUI {formatCOP(saldoNequi)}
                {usdEnCop != null
                  ? ` + USD $${saldoUsd.toFixed(2)} × ${formatCOP(tasaValor)} = ${formatCOP(usdEnCop)}`
                  : ` + USD $${saldoUsd.toFixed(2)} (tasa no configurada)`}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tarjetas por fondo */}
      {loadingFondos ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : (
        <div className="grid gap-[14px] mb-6" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))" }}>
          {fondos.map((f) => (
            <div key={f.moneda} className={`card border-l-4 ${MONEDA_BORDER_CLS[f.moneda] ?? "border-l-verde-claro"}`}>
              <div className="p-[18px] flex justify-between items-start">
                <div>
                  <div className="text-[13px] text-gray-500 mb-1">
                    {MONEDA_LABEL[f.moneda] ?? f.moneda}
                  </div>
                  <div className={`text-[28px] font-extrabold ${(f.saldo ?? 0) < 0 ? "text-red-600" : MONEDA_COLOR_CLS[f.moneda] ?? "text-verde-oscuro"}`}>
                    {f.moneda === "USD"
                      ? `$${(f.saldo ?? 0).toFixed(2)}`
                      : formatCOP(f.saldo ?? 0)}
                  </div>
                  <div className="text-[11px] text-gray-500 mt-1">
                    {f.moneda}
                    {f.moneda === "USD" && tasaValor != null && (
                      <span> · ≈ {formatCOP((f.saldo ?? 0) * tasaValor)} COP</span>
                    )}
                    {f.actualizadoEn && ` · act. ${formatHora(f.actualizadoEn)}`}
                  </div>
                </div>
                <button
                  className="btn-icon"
                  title="Ajustar saldo"
                  onClick={() => setAjustando(f)}
                >
                  <Edit2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Historial unificado */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Historial de movimientos</span>
          <span className="text-[13px] text-gray-500">{historial.length} registros</span>
        </div>
        <div className="table-wrap">
          {loadingHistorial ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : historial.length === 0 ? (
            <div className="empty-state"><p>Sin movimientos registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha / Hora</th>
                  <th>Tipo</th>
                  <th>Concepto</th>
                  <th>Moneda</th>
                  <th className="text-right">Monto</th>
                  <th className="text-right">En COP</th>
                </tr>
              </thead>
              <tbody>
                {historial.map((m, i) => {
                  const esCobro = m.tipo === "cobro";
                  return (
                    <tr key={i}>
                      <td className="text-[12px] text-gray-500 whitespace-nowrap">{formatFecha(m.fecha)}</td>
                      <td>
                        <span className={`badge gap-1 ${esCobro ? "badge-success" : "badge-danger"}`}>
                          {esCobro ? <TrendingUp size={11} /> : <TrendingDown size={11} />}
                          {esCobro ? "Cobro" : "Gasto"}
                        </span>
                      </td>
                      <td className="font-medium">{m.concepto}</td>
                      <td><span className="badge badge-warning">{m.moneda}</span></td>
                      <td className={`text-right font-semibold ${esCobro ? "text-verde-claro" : "text-red-600"}`}>
                        {esCobro ? "+" : "−"}
                        {m.moneda === "USD" ? `$${m.monto.toFixed(2)}` : formatCOP(m.monto)}
                      </td>
                      <td className={`text-right ${esCobro ? "text-verde-claro" : "text-red-600"}`}>
                        {esCobro ? "+" : "−"}{formatCOP(m.montoCop)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {ajustando && (
        <ModalAjuste
          fondo={ajustando}
          onClose={() => setAjustando(null)}
          onAjustar={(moneda, nuevoSaldo, motivo) =>
            ajustarMut.mutateAsync({ moneda, nuevoSaldo, motivo })
          }
        />
      )}
    </div>
  );
}
