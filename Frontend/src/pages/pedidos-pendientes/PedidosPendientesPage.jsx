import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { X, CreditCard, CheckCircle, Clock } from "lucide-react";
import { pedidoService } from "../../services/pedidoService";
import { formatCOP } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

function calcularInsumos(detalles = []) {
  const totales = {};
  for (const d of detalles) {
    if (!d.composicionSnapshot) continue;
    try {
      for (const { insumo, cantidad } of JSON.parse(d.composicionSnapshot)) {
        totales[insumo] = (totales[insumo] ?? 0) + cantidad * d.cantidad;
      }
    } catch {
      // Ignorar errores de parsing
    }
  }
  return totales;
}

const FRACCIONES = {
  cuy:     { 0.25: "presa", 0.5: "Mitad" },
  gallina: { 0.125: "presa", 0.5: "Mitad" },
};

function fracLabel(insumo, n) {
  const near = Object.entries(FRACCIONES[insumo] ?? {})
    .find(([v]) => Math.abs(Number(v) - n) < 0.001);
  if (near) return `${near[1]} de ${insumo}`;
  return `${parseFloat(n.toFixed(3))} ${insumo}`;
}

function formatInsumo(insumo, total) {
  if (total <= 0) return null;
  const plural = insumo === "cuy" ? "cuyes" : `${insumo}s`;
  const intPart  = Math.floor(total);
  const fracPart = Math.round((total - intPart) * 1000) / 1000;
  const parts = [];
  if (intPart > 0) parts.push(`${intPart} ${intPart === 1 ? insumo : plural}`);
  if (fracPart > 0.001) parts.push(fracLabel(insumo, fracPart));
  return parts.join(" + ");
}

// ---- Modal pago rápido ----
function ModalPago({ pedido, onClose, onPagar, onFinalizar }) {
  const [moneda, setMoneda]   = useState("COP");
  const [monto, setMonto]     = useState("");
  const [error, setError]     = useState("");
  const [loading, setLoading] = useState(false);

  const saldo = pedido.saldoPendiente ?? 0;

  const handlePago = async (e) => {
    e.preventDefault();
    if (!monto || Number(monto) <= 0) { setError("El monto debe ser mayor a 0"); return; }
    setError(""); setLoading(true);
    try {
      await onPagar(pedido.id, { moneda, monto: Number(monto) });
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message);
      setLoading(false);
    }
  };

  const handleFinalizar = async () => {
    if (!window.confirm("¿Finalizar el pedido? Se descontará el inventario.")) return;
    setError(""); setLoading(true);
    try {
      await onFinalizar(pedido.id);
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message);
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" style={{ maxWidth: 420, width: "95%" }}>
        <div className="modal-header">
          <div>
            <div className="modal-title">{pedido.nombre}</div>
            <div style={{ fontSize: 12, color: "var(--text-secondary)" }}>
              #{pedido.id}{pedido.salonNombre ? ` · ${pedido.salonNombre}` : ""}
            </div>
          </div>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error">{error}</div>}

          {/* Resumen financiero */}
          <div style={{
            background: "var(--bg-secondary)", borderRadius: 8, padding: "12px 14px",
            marginBottom: 16, display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8,
          }}>
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 11, color: "var(--text-secondary)", marginBottom: 2 }}>Total</div>
              <div style={{ fontWeight: 700, fontSize: 15 }}>{formatCOP(pedido.totalCop)}</div>
            </div>
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 11, color: "var(--text-secondary)", marginBottom: 2 }}>Abonado</div>
              <div style={{ fontWeight: 700, fontSize: 15, color: "var(--verde-claro)" }}>{formatCOP(pedido.totalAbonado)}</div>
            </div>
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 11, color: "var(--text-secondary)", marginBottom: 2 }}>
                {(pedido.valorCambio ?? 0) > 0 ? "Cambio" : "Pendiente"}
              </div>
              <div style={{ fontWeight: 700, fontSize: 15, color: (pedido.valorCambio ?? 0) > 0 ? "#d97706" : saldo > 0 ? "var(--danger)" : "var(--verde-claro)" }}>
                {(pedido.valorCambio ?? 0) > 0 ? formatCOP(pedido.valorCambio) : formatCOP(saldo)}
              </div>
            </div>
          </div>

          {/* Insumos necesarios */}
          {(() => {
            const ins = calcularInsumos(pedido.detalles);
            const gallina = ins["gallina"];
            const cuy = ins["cuy"];
            if (!gallina && !cuy) return null;
            return (
              <div style={{
                display: "flex", gap: 12, marginBottom: 12,
                padding: "6px 12px", borderRadius: 6,
                background: "rgba(52,168,83,0.07)",
                border: "1px solid rgba(52,168,83,0.2)",
                fontSize: 13, fontWeight: 600,
              }}>
                {gallina && <span>🐔 {formatInsumo("gallina", gallina)}</span>}
                {cuy     && <span>🐾 {formatInsumo("cuy", cuy)}</span>}
              </div>
            );
          })()}

          {/* Ítems del pedido */}
          {pedido.detalles?.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6, textTransform: "uppercase", letterSpacing: "0.5px" }}>
                Ítems
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                {pedido.detalles.map((d) => (
                  <div key={d.id} style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
                    <span>{d.cantidad}× {d.nombreSnapshot}</span>
                    <span style={{ color: "var(--text-secondary)" }}>{formatCOP(d.subtotalCop)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Formulario pago */}
          {saldo > 0 ? (
            <form onSubmit={handlePago}>
              <div style={{ fontSize: 12, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 8, textTransform: "uppercase", letterSpacing: "0.5px" }}>
                Registrar pago
              </div>
              <div className="form-row" style={{ gap: 8 }}>
                <div className="form-group" style={{ flex: "0 0 100px", marginBottom: 0 }}>
                  <select
                    className="form-control"
                    value={moneda}
                    onChange={(e) => setMoneda(e.target.value)}
                  >
                    <option value="COP">COP</option>
                    <option value="USD">USD</option>
                    <option value="NEQUI">NEQUI</option>
                  </select>
                </div>
                <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                  <input
                    className="form-control"
                    type="number"
                    min="1"
                    step="0.01"
                    placeholder={`Monto (saldo: ${formatCOP(saldo)})`}
                    value={monto}
                    onChange={(e) => setMonto(e.target.value)}
                    autoFocus
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ flexShrink: 0 }}
                  disabled={loading}
                >
                  <CreditCard size={14} />
                  {loading ? "…" : "Pagar"}
                </button>
              </div>
            </form>
          ) : (
            <div style={{
              background: "rgba(52, 168, 83, 0.08)", border: "1px solid var(--verde-claro)",
              borderRadius: 8, padding: "10px 14px", fontSize: 13, color: "var(--verde-claro)",
              fontWeight: 600, textAlign: "center",
            }}>
              Pago completo — listo para finalizar
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cerrar</button>
          {saldo === 0 && (
            <button
              className="btn btn-primary"
              onClick={handleFinalizar}
              disabled={loading}
            >
              <CheckCircle size={14} />
              {loading ? "Finalizando…" : "Finalizar pedido"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ---- Página principal ----
export default function PedidosPendientesPage() {
  const qc = useQueryClient();
  const { formatHora } = useFormatos();
  const [seleccionado, setSeleccionado] = useState(null);

  const { data: todos = [], isLoading } = useQuery({
    queryKey: ["pedidos", false],
    queryFn: () => pedidoService.listar(false),
    refetchInterval: 30_000,
  });

  const pendientes = todos.filter((p) => (p.totalCop ?? 0) > 0 && (p.saldoPendiente ?? 0) > 0);
  const listos     = todos.filter((p) => (p.totalCop ?? 0) > 0 && (p.saldoPendiente ?? 0) <= 0);
  const sinItems   = todos.filter((p) => (p.totalCop ?? 0) === 0);

  const pagarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.registrarPago(id, req),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["pedidos"] });
      setSeleccionado(data);
    },
  });

  const finalizarMut = useMutation({
    mutationFn: (id) => pedidoService.finalizar(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pedidos"] }),
  });

  const totalPendiente = pendientes.reduce((s, p) => s + (p.saldoPendiente ?? 0), 0);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Pedidos pendientes</h1>
          <p className="page-subtitle">Pedidos abiertos con saldo por cobrar</p>
        </div>
      </div>

      {/* KPIs */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: 12, marginBottom: 24 }}>
        <div className="card" style={{ borderLeft: "4px solid var(--danger)" }}>
          <div style={{ fontSize: 12, color: "var(--text-secondary)", marginBottom: 4 }}>Por cobrar</div>
          <div style={{ fontSize: 22, fontWeight: 800, color: "var(--danger)" }}>{formatCOP(totalPendiente)}</div>
          <div style={{ fontSize: 12, color: "var(--text-secondary)", marginTop: 2 }}>{pendientes.length} pedido{pendientes.length !== 1 ? "s" : ""}</div>
        </div>
        <div className="card" style={{ borderLeft: "4px solid var(--verde-claro)" }}>
          <div style={{ fontSize: 12, color: "var(--text-secondary)", marginBottom: 4 }}>Listos p/ finalizar</div>
          <div style={{ fontSize: 22, fontWeight: 800, color: "var(--verde-claro)" }}>{listos.length}</div>
          <div style={{ fontSize: 12, color: "var(--text-secondary)", marginTop: 2 }}>pago completo</div>
        </div>
        <div className="card" style={{ borderLeft: "4px solid var(--border)" }}>
          <div style={{ fontSize: 12, color: "var(--text-secondary)", marginBottom: 4 }}>Sin ítems</div>
          <div style={{ fontSize: 22, fontWeight: 800 }}>{sinItems.length}</div>
          <div style={{ fontSize: 12, color: "var(--text-secondary)", marginTop: 2 }}>abiertos vacíos</div>
        </div>
      </div>

      {isLoading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : pendientes.length === 0 && listos.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <CheckCircle size={40} color="var(--verde-claro)" />
            <p style={{ marginTop: 12 }}>No hay pedidos con saldo pendiente</p>
          </div>
        </div>
      ) : (
        <div className="card">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Cliente</th>
                  <th>Salón / Mesa</th>
                  <th>Reserva</th>
                  <th>Insumos</th>
                  <th style={{ textAlign: "right" }}>Total</th>
                  <th style={{ textAlign: "right" }}>Abonado</th>
                  <th style={{ textAlign: "right" }}>Pendiente</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {[...pendientes, ...listos].map((p) => {
                  const saldo = p.saldoPendiente ?? 0;
                  const listo = saldo <= 0;
                  return (
                    <tr
                      key={p.id}
                      style={{ cursor: "pointer" }}
                      onClick={() => setSeleccionado(p)}
                    >
                      <td style={{ color: "var(--text-secondary)", fontSize: 12 }}>#{p.id}</td>
                      <td style={{ fontWeight: 600 }}>{p.nombre}</td>
                      <td style={{ fontSize: 13, color: "var(--text-secondary)" }}>
                        {p.salonNombre ?? "—"}{p.mesa ? ` / ${p.mesa}` : ""}
                      </td>
                      <td style={{ fontSize: 12, whiteSpace: "nowrap" }}>
                        {p.fechaReserva ? (
                          <span style={{ fontWeight: 700, color: "var(--verde-oscuro)" }}>
                            {p.fechaReserva}{p.horaReserva ? ` ${p.horaReserva}` : ""}
                          </span>
                        ) : (
                          <span style={{ color: "var(--text-secondary)" }}>
                            <Clock size={11} style={{ marginRight: 3, verticalAlign: "middle" }} />
                            {p.creadoEn
                              ? formatHora(p.creadoEn)
                              : "—"}
                          </span>
                        )}
                      </td>
                      <td style={{ fontSize: 12 }}>
                        {(() => {
                          const ins = calcularInsumos(p.detalles);
                          const partes = [];
                          if (ins["gallina"]) partes.push(`🐔 ${formatInsumo("gallina", ins["gallina"])}`);
                          if (ins["cuy"])     partes.push(`🐾 ${formatInsumo("cuy", ins["cuy"])}`);
                          return partes.length ? partes.join(" · ") : <span style={{ color: "var(--text-secondary)" }}>—</span>;
                        })()}
                      </td>
                      <td style={{ textAlign: "right", fontWeight: 600 }}>{formatCOP(p.totalCop)}</td>
                      <td style={{ textAlign: "right", color: "var(--verde-claro)" }}>{formatCOP(p.totalAbonado)}</td>
                      <td style={{ textAlign: "right" }}>
                        {listo ? (
                          <span className="badge badge-success">Pagado</span>
                        ) : (
                          <span style={{ fontWeight: 700, color: "var(--danger)" }}>{formatCOP(saldo)}</span>
                        )}
                      </td>
                      <td>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={(e) => { e.stopPropagation(); setSeleccionado(p); }}
                        >
                          {listo
                            ? <><CheckCircle size={13} /> Finalizar</>
                            : <><CreditCard size={13} /> Cobrar</>}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {seleccionado && (
        <ModalPago
          pedido={seleccionado}
          onClose={() => setSeleccionado(null)}
          onPagar={(id, req) => pagarMut.mutateAsync({ id, req })}
          onFinalizar={(id) => finalizarMut.mutateAsync(id)}
        />
      )}
    </div>
  );
}
