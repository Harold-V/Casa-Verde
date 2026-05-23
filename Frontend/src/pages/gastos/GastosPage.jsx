import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, X, Trash2 } from "lucide-react";
import { gastoService } from "../../services/gastoService";
import { formatCOP } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

const CATEGORIAS = ["insumos", "servicios", "equipos", "otros"];

const CAT_BADGE = {
  insumos:   "badge-success",
  servicios: "badge-warning",
  equipos:   "badge-gray",
  otros:     "badge-gray",
};

const HOY = new Date().toISOString().slice(0, 10);

function ModalGasto({ onClose, onGuardar }) {
  const [form, setForm] = useState({
    concepto: "", categoria: "insumos", moneda: "COP", monto: "", observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      await onGuardar({ ...form, monto: Number(form.monto) });
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message);
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[480px]">
        <div className="modal-header">
          <span className="modal-title">Registrar gasto</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-group">
              <label className="form-label">Concepto</label>
              <input
                className="form-control" autoFocus
                value={form.concepto}
                onChange={(e) => set("concepto", e.target.value)}
                placeholder="ej. Compra 10 gallinas"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Categoría</label>
                <select className="form-control" value={form.categoria} onChange={(e) => set("categoria", e.target.value)}>
                  {CATEGORIAS.map((c) => (
                    <option key={c} value={c}>{c.charAt(0).toUpperCase() + c.slice(1)}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Moneda</label>
                <select className="form-control" value={form.moneda} onChange={(e) => set("moneda", e.target.value)}>
                  <option value="COP">COP — Pesos</option>
                  <option value="USD">USD — Dólares</option>
                  <option value="NEQUI">NEQUI</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">
                Monto {form.moneda === "USD" ? "(en dólares)" : "(en pesos)"}
              </label>
              <input
                className="form-control" type="number" min="0" step="0.01"
                value={form.monto}
                onChange={(e) => set("monto", e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Observación (opcional)</label>
              <input
                className="form-control"
                value={form.observacion}
                onChange={(e) => set("observacion", e.target.value)}
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Registrar gasto"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function GastosPage() {
  const qc = useQueryClient();
  const { formatFecha } = useFormatos();
  const [modalOpen, setModalOpen] = useState(false);
  const [filtroFecha, setFiltroFecha] = useState(HOY);

  const { data: gastos = [], isLoading } = useQuery({
    queryKey: ["gastos", filtroFecha],
    queryFn: () => gastoService.listar(filtroFecha || undefined),
  });

  const registrarMut = useMutation({
    mutationFn: gastoService.registrar,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["gastos"] }),
  });

  const eliminarMut = useMutation({
    mutationFn: gastoService.eliminar,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["gastos"] }),
  });

  const handleEliminar = (id, concepto) => {
    if (!window.confirm(`¿Eliminar el gasto "${concepto}"? Se revertirá el fondo.`)) return;
    eliminarMut.mutate(id);
  };

  const totalHoy   = gastos.reduce((s, g) => s + (g.montoCop ?? 0), 0);
  const totalCop   = gastos.filter((g) => g.moneda === "COP").reduce((s, g) => s + g.monto, 0);
  const totalUsd   = gastos.filter((g) => g.moneda === "USD").reduce((s, g) => s + g.monto, 0);
  const totalNequi = gastos.filter((g) => g.moneda === "NEQUI").reduce((s, g) => s + g.monto, 0);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Gastos</h1>
        <div className="flex gap-2.5 items-center">
          <div className="flex items-center gap-1.5">
            <label className="form-label mb-0 text-[13px]">Fecha:</label>
            <input
              type="date" className="form-control w-[150px]"
              value={filtroFecha}
              onChange={(e) => setFiltroFecha(e.target.value)}
            />
            {filtroFecha && (
              <button className="btn btn-secondary btn-sm" onClick={() => setFiltroFecha("")}>
                Ver todos
              </button>
            )}
          </div>
          <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
            <Plus size={15} /> Registrar gasto
          </button>
        </div>
      </div>

      {/* KPIs */}
      <div className="kpi-grid mb-5">
        <div className="kpi-card border-l-red-600">
          <div className="kpi-label">Total en COP equivalente</div>
          <div className="kpi-value">{formatCOP(totalHoy)}</div>
        </div>
        {totalCop > 0 && (
          <div className="kpi-card">
            <div className="kpi-label">Efectivo COP</div>
            <div className="kpi-value">{formatCOP(totalCop)}</div>
          </div>
        )}
        {totalUsd > 0 && (
          <div className="kpi-card">
            <div className="kpi-label">USD</div>
            <div className="kpi-value">${totalUsd.toFixed(2)}</div>
          </div>
        )}
        {totalNequi > 0 && (
          <div className="kpi-card">
            <div className="kpi-label">NEQUI</div>
            <div className="kpi-value">{formatCOP(totalNequi)}</div>
          </div>
        )}
      </div>

      {/* Tabla */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">
            {filtroFecha ? `Gastos del ${filtroFecha}` : "Todos los gastos"}
          </span>
          <span className="text-[13px] text-gray-500">
            {gastos.length} registro{gastos.length !== 1 ? "s" : ""}
          </span>
        </div>
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : gastos.length === 0 ? (
            <div className="empty-state">
              <p>Sin gastos registrados{filtroFecha ? " para esta fecha" : ""}</p>
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha / Hora</th>
                  <th>Concepto</th>
                  <th>Categoría</th>
                  <th>Moneda</th>
                  <th className="text-right">Monto</th>
                  <th className="text-right">En COP</th>
                  <th>Obs.</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {gastos.map((g) => (
                  <tr key={g.id}>
                    <td className="text-[12px] text-gray-500 whitespace-nowrap">{formatFecha(g.registradoEn)}</td>
                    <td className="font-medium">{g.concepto}</td>
                    <td>
                      <span className={`badge ${CAT_BADGE[g.categoria] ?? "badge-gray"}`}>{g.categoria}</span>
                    </td>
                    <td>
                      <span className="badge badge-warning">{g.moneda}</span>
                    </td>
                    <td className="text-right font-semibold">
                      {g.moneda === "USD" ? `$${g.monto.toFixed(2)}` : formatCOP(g.monto)}
                    </td>
                    <td className="text-right">{formatCOP(g.montoCop)}</td>
                    <td className="text-[12px] text-gray-500">{g.observacion ?? "—"}</td>
                    <td>
                      <button
                        className="btn-icon"
                        title="Eliminar y revertir fondo"
                        onClick={() => handleEliminar(g.id, g.concepto)}
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {modalOpen && (
        <ModalGasto
          onClose={() => setModalOpen(false)}
          onGuardar={(data) => registrarMut.mutateAsync(data)}
        />
      )}
    </div>
  );
}
