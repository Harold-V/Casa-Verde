import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { tasaService } from "../../../services/tasaService";
import { formatCOP, formatFecha } from "../../../utils/format";

export default function TasaUsdTab() {
  const qc = useQueryClient();
  const [nuevaTasa, setNuevaTasa] = useState("");
  const [msg, setMsg] = useState(null);

  const { data: vigente } = useQuery({ queryKey: ["tasa-vigente"], queryFn: tasaService.vigente });
  const { data: historial = [] } = useQuery({ queryKey: ["tasa-historial"], queryFn: tasaService.historial });

  const mutRegistrar = useMutation({
    mutationFn: () => tasaService.registrar(parseFloat(nuevaTasa)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasa-vigente"] });
      qc.invalidateQueries({ queryKey: ["tasa-historial"] });
      setNuevaTasa("");
      setMsg({ tipo: "success", texto: "Tasa actualizada correctamente" });
      setTimeout(() => setMsg(null), 3000);
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const handleGuardar = (e) => {
    e.preventDefault();
    const val = parseFloat(nuevaTasa);
    if (!nuevaTasa || isNaN(val) || val <= 0) {
      setMsg({ tipo: "error", texto: "Ingrese un valor válido mayor a 0" });
      return;
    }
    mutRegistrar.mutate();
  };

  return (
    <div>
      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-header">
          <span className="card-title">Tasa vigente del dólar</span>
        </div>
        <div className="card-body">
          {vigente ? (
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: "var(--verde-oscuro)" }}>
                {formatCOP(vigente.valorCop)} <span style={{ fontSize: 14, color: "var(--text-secondary)", fontWeight: 400 }}>COP / USD</span>
              </div>
              <div style={{ color: "var(--text-secondary)", fontSize: 13, marginTop: 4 }}>
                Vigente desde: {formatFecha(vigente.vigentDesde)}
              </div>
            </div>
          ) : (
            <div className="alert alert-error">No hay tasa registrada</div>
          )}

          {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

          <form onSubmit={handleGuardar} style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
            <div className="form-group" style={{ marginBottom: 0, flex: 1 }}>
              <label className="form-label">Nueva tasa (COP por 1 USD)</label>
              <input
                className="form-control"
                type="number"
                step="1"
                min="1"
                placeholder="Ej: 4200"
                value={nuevaTasa}
                onChange={(e) => setNuevaTasa(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={mutRegistrar.isPending}>
              {mutRegistrar.isPending ? "Guardando…" : "Guardar tasa"}
            </button>
          </form>
        </div>
      </div>

      <div className="card">
        <div className="card-header"><span className="card-title">Historial de tasas</span></div>
        <div className="table-wrap">
          {historial.length === 0 ? (
            <div className="empty-state"><p>Sin historial</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Tasa (COP/USD)</th><th>Vigente desde</th></tr>
              </thead>
              <tbody>
                {historial.map((t, i) => (
                  <tr key={t.id}>
                    <td style={{ fontWeight: i === 0 ? 700 : 400 }}>{formatCOP(t.valorCop)}</td>
                    <td style={{ color: "var(--text-secondary)" }}>{formatFecha(t.vigentDesde)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
