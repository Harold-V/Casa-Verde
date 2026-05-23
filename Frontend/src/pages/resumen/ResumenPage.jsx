import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { TrendingUp, TrendingDown, DollarSign, ShoppingBag } from "lucide-react";
import { resumenService } from "../../services/resumenService";
import { formatCOP } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

const HOY = new Date().toISOString().slice(0, 10);

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:          "badge-success",
  FINALIZADO:      "badge-info",
  ANULADO:         "badge-danger",
};

const CAT_BADGE = {
  insumos:   "badge-success",
  servicios: "badge-warning",
  equipos:   "badge-gray",
  otros:     "badge-gray",
};

const COLOR_CLS = {
  "var(--verde-claro)":    { border: "border-l-verde-claro", text: "text-verde-claro" },
  "var(--danger)":         { border: "border-l-red-600",     text: "text-red-600" },
  "var(--warning)":        { border: "border-l-amber-500",   text: "text-amber-600" },
  "var(--text-secondary)": { border: "border-l-gray-300",    text: "text-gray-400" },
};

function KpiCard({ label, value, sub, color, icon: Icon }) {
  const cls = COLOR_CLS[color] ?? {};
  return (
    <div className={`kpi-card ${cls.border ?? ""}`}>
      <div className="flex justify-between items-start">
        <div>
          <div className="kpi-label">{label}</div>
          <div className={`kpi-value ${cls.text ?? ""}`}>{value}</div>
          {sub && <div className="text-[12px] text-gray-500 mt-0.5">{sub}</div>}
        </div>
        {Icon && <Icon size={22} className={`${cls.text ?? "text-verde-claro"} opacity-60`} />}
      </div>
    </div>
  );
}

export default function ResumenPage() {
  const { formatFecha, formatHora } = useFormatos();
  const [fecha, setFecha] = useState(HOY);

  const { data: r, isLoading, error } = useQuery({
    queryKey: ["resumen", fecha],
    queryFn: () => resumenService.obtener(fecha),
    staleTime: 10_000,
  });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Resumen del día</h1>
        <div className="flex items-center gap-2">
          <label className="form-label mb-0 text-[13px]">Fecha:</label>
          <input
            type="date" className="form-control w-40"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
          {fecha !== HOY && (
            <button className="btn btn-secondary btn-sm" onClick={() => setFecha(HOY)}>Hoy</button>
          )}
        </div>
      </div>

      {isLoading && <div className="loading-center"><div className="spinner" /></div>}
      {error && <div className="alert alert-error">Error cargando resumen</div>}

      {r && (
        <>
          {/* Pedidos del día */}
          <div className="font-semibold text-verde-oscuro mb-2">Pedidos</div>
          <div className="kpi-grid mb-5">
            <KpiCard label="Total pedidos" value={r.pedidosTotales} icon={ShoppingBag} />
            <KpiCard label="Finalizados" value={r.pedidosFinalizados} color="var(--verde-claro)" />
            <KpiCard
              label="Abiertos"
              value={r.pedidosAbiertos}
              color={r.pedidosAbiertos > 0 ? "var(--warning)" : "var(--verde-claro)"}
            />
            <KpiCard
              label="Anulados"
              value={r.pedidosAnulados}
              color={r.pedidosAnulados > 0 ? "var(--danger)" : "var(--text-secondary)"}
            />
          </div>

          {/* Cobros del día */}
          <div className="font-semibold text-verde-oscuro mb-2">Cobros del día</div>
          <div className="kpi-grid mb-5">
            <KpiCard
              label="Total cobrado"
              value={formatCOP(r.cobradoTotalCop)}
              color="var(--verde-claro)"
              icon={TrendingUp}
            />
            {r.cobradoCop > 0 && (
              <KpiCard label="Efectivo COP" value={formatCOP(r.cobradoCop)} />
            )}
            {r.cobradoUsd > 0 && (
              <KpiCard
                label="Dólares USD"
                value={`$${r.cobradoUsd.toFixed(2)}`}
                sub={`≈ ${formatCOP(r.cobradoUsd * (r.cobradoTotalCop / (r.cobradoUsd || 1)))}`}
              />
            )}
            {r.cobradoNequi > 0 && (
              <KpiCard label="NEQUI" value={formatCOP(r.cobradoNequi)} />
            )}
          </div>

          {/* Gastos + Balance */}
          <div className="font-semibold text-verde-oscuro mb-2">Gastos y balance</div>
          <div className="kpi-grid mb-6">
            <KpiCard
              label="Total gastos"
              value={formatCOP(r.gastosTotalCop)}
              color="var(--danger)"
              icon={TrendingDown}
            />
            {r.gastosCop > 0 && (
              <KpiCard label="Gastos COP" value={formatCOP(r.gastosCop)} color="var(--danger)" />
            )}
            {r.gastosUsd > 0 && (
              <KpiCard label="Gastos USD" value={`$${r.gastosUsd.toFixed(2)}`} color="var(--danger)" />
            )}
            <KpiCard
              label="Balance neto"
              value={formatCOP(r.balanceNeto)}
              color={r.balanceNeto >= 0 ? "var(--verde-claro)" : "var(--danger)"}
              icon={DollarSign}
            />
          </div>

          {/* Fondos actuales */}
          {r.fondos && r.fondos.length > 0 && (
            <>
              <div className="font-semibold text-verde-oscuro mb-2">Fondos actuales</div>
              <div className="kpi-grid mb-6">
                {r.fondos.map((f) => (
                  <KpiCard
                    key={f.moneda}
                    label={`Fondo ${f.moneda}`}
                    value={f.moneda === "USD" ? `$${(f.saldo ?? 0).toFixed(2)} USD` : formatCOP(f.saldo ?? 0)}
                    color={f.saldo < 0 ? "var(--danger)" : "var(--verde-claro)"}
                  />
                ))}
              </div>
            </>
          )}

          {/* Tabla pedidos del día */}
          <div className="card mb-4">
            <div className="card-header">
              <span className="card-title">Pedidos del {fecha}</span>
              <span className="text-[13px] text-gray-500">{r.pedidos.length} pedido{r.pedidos.length !== 1 ? "s" : ""}</span>
            </div>
            <div className="table-wrap">
              {r.pedidos.length === 0 ? (
                <div className="empty-state"><p>Sin pedidos para esta fecha</p></div>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Salón / Mesa</th>
                      <th>Estado</th>
                      <th>Ítems</th>
                      <th className="text-right">Total</th>
                      <th className="text-right">Abonado</th>
                      <th className="text-right">Saldo</th>
                      <th>Hora</th>
                    </tr>
                  </thead>
                  <tbody>
                    {r.pedidos.map((p) => (
                      <tr key={p.id}>
                        <td className="font-bold">#{p.id}</td>
                        <td className="text-gray-500 text-[13px]">
                          {[p.salonNombre, p.mesa].filter(Boolean).join(" / ") || "—"}
                        </td>
                        <td>
                          <span className={`badge ${ESTADO_BADGE[p.estado] ?? "badge-gray"}`}>{p.estado}</span>
                        </td>
                        <td className="text-center">{p.detalles?.length ?? 0}</td>
                        <td className="text-right font-semibold">{formatCOP(p.totalCop)}</td>
                        <td className="text-right text-verde-claro">{formatCOP(p.totalAbonado)}</td>
                        <td className={`text-right font-semibold ${p.saldoPendiente > 0 ? "text-red-600" : (p.valorCambio ?? 0) > 0 ? "text-amber-600" : "text-verde-claro"}`}>
                          {(p.valorCambio ?? 0) > 0
                            ? `+${formatCOP(p.valorCambio)} cambio`
                            : formatCOP(p.saldoPendiente)}
                        </td>
                        <td className="text-[12px] text-gray-500">
                          {p.estado === "FINALIZADO" && p.finalizadoEn
                            ? formatHora(p.finalizadoEn)
                            : formatHora(p.creadoEn)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>

          {/* Tabla gastos del día */}
          <div className="card">
            <div className="card-header">
              <span className="card-title">Gastos del {fecha}</span>
              <span className="text-[13px] text-gray-500">{r.gastos.length} registro{r.gastos.length !== 1 ? "s" : ""}</span>
            </div>
            <div className="table-wrap">
              {r.gastos.length === 0 ? (
                <div className="empty-state"><p>Sin gastos registrados para esta fecha</p></div>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Concepto</th>
                      <th>Categoría</th>
                      <th>Moneda</th>
                      <th className="text-right">Monto</th>
                      <th className="text-right">En COP</th>
                      <th>Hora</th>
                    </tr>
                  </thead>
                  <tbody>
                    {r.gastos.map((g) => (
                      <tr key={g.id}>
                        <td className="font-medium">{g.concepto}</td>
                        <td><span className={`badge ${CAT_BADGE[g.categoria] ?? "badge-gray"}`}>{g.categoria}</span></td>
                        <td><span className="badge badge-warning">{g.moneda}</span></td>
                        <td className="text-right font-semibold">
                          {g.moneda === "USD" ? `$${g.monto.toFixed(2)}` : formatCOP(g.monto)}
                        </td>
                        <td className="text-right">{formatCOP(g.montoCop)}</td>
                        <td className="text-[12px] text-gray-500">{formatHora(g.registradoEn)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
