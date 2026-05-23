import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, X, ArrowDown, ArrowUp, SlidersHorizontal } from "lucide-react";
import { inventarioService } from "../../services/inventarioService";
import { formatDecimal } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

const TIPO_CONFIG = {
  entrada: { label: "Entrada",  badge: "badge-success", icono: <ArrowDown size={13} /> },
  salida:  { label: "Salida",   badge: "badge-danger",  icono: <ArrowUp size={13} /> },
  ajuste:  { label: "Ajuste",   badge: "badge-warning", icono: <SlidersHorizontal size={13} /> },
};

function ModalMovimiento({ insumos, onClose, onSave }) {
  const [form, setForm] = useState({ insumo: insumos[0]?.insumo ?? "gallina", tipo: "entrada", cantidad: "", observacion: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    const cantidad = parseFloat(form.cantidad);
    if (isNaN(cantidad) || cantidad <= 0) { setError("La cantidad debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await onSave({ ...form, cantidad });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const stockActual = insumos.find((i) => i.insumo === form.insumo)?.stockActual ?? 0;

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar movimiento de inventario</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Insumo</label>
                <select className="form-control" value={form.insumo} onChange={(e) => set("insumo", e.target.value)}>
                  {insumos.map((i) => <option key={i.insumo} value={i.insumo}>{i.insumo}</option>)}
                  <option value="gallina">gallina</option>
                  <option value="cuy">cuy</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Tipo</label>
                <select className="form-control" value={form.tipo} onChange={(e) => set("tipo", e.target.value)}>
                  <option value="entrada">Entrada (compra/ingreso)</option>
                  <option value="salida">Salida (manual)</option>
                  <option value="ajuste">Ajuste (nuevo stock exacto)</option>
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">
                Cantidad{form.tipo === "ajuste" ? " (stock final)" : ""}
                {" "}— Stock actual: <strong>{formatDecimal(stockActual)}</strong>
              </label>
              <input className="form-control" type="number" step="0.001" min="0" value={form.cantidad} onChange={(e) => set("cantidad", e.target.value)} autoFocus />
            </div>
            <div className="form-group">
              <label className="form-label">Observación (opcional)</label>
              <input className="form-control" value={form.observacion} onChange={(e) => set("observacion", e.target.value)} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? "Guardando…" : "Registrar"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function InventarioPage() {
  const qc = useQueryClient();
  const { formatFecha } = useFormatos();
  const [modalOpen, setModalOpen] = useState(false);
  const [filtroInsumo, setFiltroInsumo] = useState("");

  const { data: stock = [], isLoading: loadingStock } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
  });

  const { data: movimientos = [], isLoading: loadingMov } = useQuery({
    queryKey: ["inventario-movimientos", filtroInsumo],
    queryFn: () => inventarioService.listarMovimientos(filtroInsumo || undefined),
  });

  const mutMovimiento = useMutation({
    mutationFn: inventarioService.registrarMovimiento,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["inventario-stock"] });
      qc.invalidateQueries({ queryKey: ["inventario-movimientos"] });
    },
  });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Inventario</h1>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          <Plus size={15} /> Registrar movimiento
        </button>
      </div>

      {/* Stock actual */}
      <div className="kpi-grid mb-6">
        {loadingStock ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : stock.map((s) => {
          const borderCls = s.stockActual < 2 ? "border-l-red-600" : s.stockActual < 5 ? "border-l-amber-500" : "border-l-verde-claro";
          return (
            <div key={s.insumo} className={`kpi-card ${borderCls}`}>
              <div className="kpi-label">{s.insumo}</div>
              <div className="kpi-value">{formatDecimal(s.stockActual)}</div>
              <div className="text-[12px] text-gray-500 mt-0.5">{s.unidad}</div>
            </div>
          );
        })}
      </div>

      {/* Movimientos */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Historial de movimientos</span>
          <div className="flex gap-2.5 items-center">
            <select className="form-control w-40" value={filtroInsumo} onChange={(e) => setFiltroInsumo(e.target.value)}>
              <option value="">Todos los insumos</option>
              {stock.map((s) => <option key={s.insumo} value={s.insumo}>{s.insumo}</option>)}
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
                {movimientos.map((m) => {
                  const conf = TIPO_CONFIG[m.tipo] ?? { label: m.tipo, badge: "badge-gray", icono: null };
                  return (
                    <tr key={m.id}>
                      <td className="text-gray-500 text-[12px] whitespace-nowrap">{formatFecha(m.registradoEn)}</td>
                      <td className="font-medium">{m.insumo}</td>
                      <td>
                        <span className={`badge ${conf.badge} gap-1`}>
                          {conf.icono} {conf.label}
                        </span>
                      </td>
                      <td className="font-semibold">
                        {m.tipo === "ajuste" ? "→" : m.tipo === "entrada" ? "+" : "-"}{formatDecimal(m.cantidad)}
                      </td>
                      <td>{formatDecimal(m.stockResultante)}</td>
                      <td className="text-gray-500 text-[12px]">
                        {m.referenciaTipo ? `${m.referenciaTipo}${m.referenciaId ? " #" + m.referenciaId : ""}` : "—"}
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

      {modalOpen && (
        <ModalMovimiento
          insumos={stock}
          onClose={() => setModalOpen(false)}
          onSave={(data) => mutMovimiento.mutateAsync(data)}
        />
      )}
    </div>
  );
}
