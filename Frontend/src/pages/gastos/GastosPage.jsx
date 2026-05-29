import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { gastoService } from "../../services/gastoService";
import { formatCOP, formatFecha, formatUSD } from "../../utils/format";
import { CAT_BADGE } from "../../utils/constants";
import ConfirmDialog from "../../components/shared/ConfirmDialog";
import ModalGasto from "../../components/gastos/ModalGasto";

export default function GastosPage() {
  const qc = useQueryClient();
  const hoy = new Date().toLocaleDateString("en-CA");
  const [modalOpen, setModalOpen] = useState(false);
  const [filtroFecha, setFiltroFecha] = useState(hoy);
  const [confirmEliminar, setConfirmEliminar] = useState(null); // { id, concepto }
  const [errorEliminar, setErrorEliminar] = useState("");

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
    onSuccess: () => {
      setErrorEliminar("");
      qc.invalidateQueries({ queryKey: ["gastos"] });
    },
    onError: (err) => setErrorEliminar(err.message),
  });

  const handleEliminar = (id, concepto) => {
    setConfirmEliminar({ id, concepto });
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
            <div className="kpi-value">{formatUSD(totalUsd)}</div>
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
                      <span className={`badge ${CAT_BADGE[g.categoria] ?? "badge-gray"}`}>{g.categoria ? g.categoria.charAt(0).toUpperCase() + g.categoria.slice(1) : ""}</span>
                    </td>
                    <td>
                      <span className="badge badge-warning">{g.moneda}</span>
                    </td>
                    <td className="text-right font-semibold">
                      {g.moneda === "USD" ? formatUSD(g.monto) : formatCOP(g.monto)}
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

      {errorEliminar && (
        <div className="alert alert-error mt-4">{errorEliminar}</div>
      )}

      {modalOpen && (
        <ModalGasto
          onClose={() => setModalOpen(false)}
          onGuardar={(data) => registrarMut.mutateAsync(data)}
        />
      )}

      {confirmEliminar && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="¿Eliminar gasto?"
          descripcion={`Se eliminará "${confirmEliminar.concepto}" y se revertirá el fondo correspondiente.`}
          labelOk="Sí, eliminar"
          peligro
          onOk={() => { eliminarMut.mutate(confirmEliminar.id); setConfirmEliminar(null); }}
          onCancelar={() => setConfirmEliminar(null)}
        />
      )}
    </div>
  );
}
