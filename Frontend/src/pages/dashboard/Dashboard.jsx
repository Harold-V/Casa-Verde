import { useQuery } from "@tanstack/react-query";
import { inventarioService } from "../../services/inventarioService";
import { formatDecimal } from "../../utils/format";

export default function Dashboard() {
  const { data: stock = [] } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
  });

  const gallina = stock.find((s) => s.insumo === "gallina");
  const cuy = stock.find((s) => s.insumo === "cuy");

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
      </div>

      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">Gallinas en stock</div>
          <div className="kpi-value">{formatDecimal(gallina?.stockActual ?? 0)}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Cuyes en stock</div>
          <div className="kpi-value">{formatDecimal(cuy?.stockActual ?? 0)}</div>
        </div>
        <div className="kpi-card" style={{ borderLeftColor: "var(--warning)" }}>
          <div className="kpi-label">Pedidos activos</div>
          <div className="kpi-value">—</div>
        </div>
        <div className="kpi-card" style={{ borderLeftColor: "var(--success)" }}>
          <div className="kpi-label">Total vendido hoy</div>
          <div className="kpi-value">—</div>
        </div>
        <div className="kpi-card" style={{ borderLeftColor: "var(--danger)" }}>
          <div className="kpi-label">Gastos hoy</div>
          <div className="kpi-value">—</div>
        </div>
        <div className="kpi-card" style={{ borderLeftColor: "var(--info)" }}>
          <div className="kpi-label">Balance del día</div>
          <div className="kpi-value">—</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Pedidos activos</span>
        </div>
        <div className="card-body empty-state">
          <p>Los pedidos activos aparecerán aquí — Sprint 2</p>
        </div>
      </div>
    </div>
  );
}
