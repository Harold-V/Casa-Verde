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
    if (!isFinite(monto) || monto <= 0) { setError("El monto debe ser mayor a 0"); return; }

    const plural   = form.insumo === "cuy" ? "cuyes" : `${form.insumo}s`;
    const concepto = `Compra ${cantidad} ${plural}`;

    setSaving(true);
    try {
      await gastoService.registrar({
        concepto,
        categoria:          "insumos",
        moneda:             form.moneda,
        monto,
        insumoInventario:   form.insumo,
        cantidadInventario: cantidad,
      });
      queryClient.invalidateQueries({ queryKey: ["inventario-stock"]       });
      queryClient.invalidateQueries({ queryKey: ["inventario-movimientos"] });
      queryClient.invalidateQueries({ queryKey: ["gastos"]                 });
      queryClient.invalidateQueries({ queryKey: ["fondos"]                 });
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
                  step={form.moneda === "USD" ? "0.01" : "1"}
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
