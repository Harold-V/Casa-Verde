import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, AlertTriangle } from "lucide-react";
import { resumenService } from "../../services/resumenService";
import { pedidoService } from "../../services/pedidoService";
import { inventarioService } from "../../services/inventarioService";
import { formatCOP, formatDecimal } from "../../utils/format";
import { useFormatos } from "../../hooks/useFormatos";

const HOY = new Date().toISOString().slice(0, 10);

// color → Tailwind border-l + text classes
const COLOR_CLS = {
  "var(--warning)":        { border: "border-l-amber-500",   text: "text-amber-600" },
  "var(--danger)":         { border: "border-l-red-600",     text: "text-red-600" },
  "var(--verde-claro)":    { border: "border-l-verde-claro", text: "text-verde-claro" },
  "var(--text-secondary)": { border: "border-l-gray-300",    text: "text-gray-400" },
};

function KpiCard({ label, value, sub, color, dim }) {
  const cls = COLOR_CLS[color] ?? {};
  return (
    <div className={`kpi-card ${cls.border ?? ""} ${dim ? "opacity-50" : ""}`}>
      <div className="kpi-label">{label}</div>
      <div className={`kpi-value ${cls.text ?? ""}`}>{value}</div>
      {sub && <div className="text-[12px] text-gray-500 mt-0.5">{sub}</div>}
    </div>
  );
}

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:          "badge-success",
  FINALIZADO:      "badge-info",
  ANULADO:         "badge-danger",
};

export default function Dashboard() {
  const navigate = useNavigate();
  const { formatHora } = useFormatos();

  const { data: resumen } = useQuery({
    queryKey: ["resumen", HOY],
    queryFn: () => resumenService.obtener(HOY),
    staleTime: 15_000,
    refetchInterval: 30_000,
  });

  const { data: pedidosActivos = [] } = useQuery({
    queryKey: ["pedidos", false],
    queryFn: () => pedidoService.listar(false),
    staleTime: 10_000,
    refetchInterval: 20_000,
  });

  const { data: stock = [] } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
    staleTime: 30_000,
  });

  const r = resumen;
  const porCobrar = pedidosActivos.reduce((s, p) => s + (p.saldoPendiente ?? 0), 0);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <span className="text-[13px] text-gray-500">
          {new Date().toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}
        </span>
      </div>

      {/* ---- Operaciones del día ---- */}
      <div className="font-semibold text-verde-oscuro mb-2 text-[13px]">Operaciones del día</div>
      <div className="kpi-grid mb-5">
        <KpiCard
          label="Pedidos activos"
          value={pedidosActivos.length}
          color={pedidosActivos.length > 0 ? "var(--warning)" : "var(--verde-claro)"}
          sub={pedidosActivos.length > 0 ? "activos ahora" : "ninguno activo"}
        />
        <KpiCard
          label="Por cobrar"
          value={formatCOP(porCobrar)}
          color={porCobrar > 0 ? "var(--warning)" : "var(--verde-claro)"}
        />
        <KpiCard
          label="Pedidos finalizados hoy"
          value={r?.pedidosFinalizados ?? "—"}
          sub={r ? `de ${r.pedidosTotales} creados` : undefined}
        />
        <KpiCard
          label="Cobrado hoy"
          value={r ? formatCOP(r.cobradoTotalCop) : "—"}
          color="var(--verde-claro)"
        />
        <KpiCard
          label="Gastos hoy"
          value={r ? formatCOP(r.gastosTotalCop) : "—"}
          color={r?.gastosTotalCop > 0 ? "var(--danger)" : undefined}
        />
      </div>

      {/* ---- Stock ---- */}
      {stock.length > 0 && (
        <>
          <div className="font-semibold text-verde-oscuro mb-2 text-[13px]">Inventario</div>
          <div className="kpi-grid mb-6">
            {stock.map((s) => (
              <KpiCard
                key={s.insumo}
                label={`Stock ${s.insumo}`}
                value={formatDecimal(s.stockActual)}
                sub={s.unidad}
                color={s.stockActual < 2 ? "var(--danger)" : s.stockActual < 5 ? "var(--warning)" : "var(--verde-claro)"}
              />
            ))}
          </div>
        </>
      )}

      {/* ---- Pedidos abiertos ---- */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Pedidos activos</span>
          <button
            className="btn btn-secondary btn-sm flex items-center gap-1"
            onClick={() => navigate("/pedidos")}
          >
            Ver todos <ArrowRight size={13} />
          </button>
        </div>
        <div className="table-wrap">
          {pedidosActivos.length === 0 ? (
            <div className="empty-state"><p>No hay pedidos activos en este momento</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Cliente</th>
                  <th>Salón / Mesa</th>
                  <th>Ítems</th>
                  <th className="text-right">Total</th>
                  <th className="text-right">Saldo</th>
                  <th>Hora</th>
                </tr>
              </thead>
              <tbody>
                {pedidosActivos.map((p) => (
                  <tr
                    key={p.id}
                    className="cursor-pointer"
                    onClick={() => navigate("/pedidos")}
                    title="Ir a Pedidos"
                  >
                    <td className="font-bold text-gray-500 text-[12px]">#{p.id}</td>
                    <td className="font-semibold">{p.nombre ?? "—"}</td>
                    <td className="text-[13px] text-gray-500">
                      {[p.salonNombre, p.mesa].filter(Boolean).join(" / ") || "—"}
                    </td>
                    <td className="text-center">{p.detalles?.length ?? 0}</td>
                    <td className="text-right font-semibold">{formatCOP(p.totalCop)}</td>
                    <td className={`text-right font-bold ${p.saldoPendiente > 0 ? "text-red-600" : "text-verde-claro"}`}>
                      {formatCOP(p.saldoPendiente)}
                    </td>
                    <td className="text-[12px] text-gray-500">{formatHora(p.creadoEn)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Alerta de stock bajo */}
      {stock.some((s) => s.stockActual < 2) && (
        <div className="mt-3.5 px-[14px] py-[10px] rounded-lg bg-red-600/[0.08] border border-red-600 flex items-center gap-2.5 text-[13px] text-red-600">
          <AlertTriangle size={16} />
          <span>
            <strong>Stock crítico:</strong>{" "}
            {stock.filter((s) => s.stockActual < 2).map((s) => `${s.insumo} (${formatDecimal(s.stockActual)})`).join(", ")}
          </span>
        </div>
      )}
    </div>
  );
}
