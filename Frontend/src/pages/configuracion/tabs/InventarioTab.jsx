import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { inventarioService } from "../../../services/inventarioService";
import { formatDecimal } from "../../../utils/format";
import { capFirst } from "../../../utils/pedidoHelpers";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";

export default function InventarioTab() {
  const qc = useQueryClient();

  const { data: stock = [], isLoading } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
  });

  const handleAjustado = () => qc.invalidateQueries({ queryKey: ["inventario-stock"] });

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">Ajuste de stock</span>
      </div>
      <div className="card-body">
        <p className="text-sm text-gray-500 mb-4">
          Establece el stock exacto de cada insumo. Úsalo para configuración inicial o
          corrección de inventario.
        </p>
        {isLoading ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : (
          <div className="flex flex-col">
            {stock.map((s) => (
              <InsumoRow key={s.insumo} insumo={s} onAjustado={handleAjustado} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function InsumoRow({ insumo, onAjustado }) {
  const [cantidad, setCantidad] = useState("");
  const [msg, setMsg] = useState(null);
  const [confirmando, setConfirmando] = useState(false);

  const mut = useMutation({
    mutationFn: () =>
      inventarioService.registrarMovimiento({
        insumo: insumo.insumo,
        tipo: "ajuste",
        cantidad: parseFloat(cantidad),
        observacion: "Ajuste manual desde configuración",
      }),
    onSuccess: () => {
      setCantidad("");
      setMsg({ tipo: "ok", texto: "Stock actualizado" });
      onAjustado();
      setTimeout(() => setMsg(null), 3000);
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    const val = parseFloat(cantidad);
    if (isNaN(val) || val < 0) {
      setMsg({ tipo: "error", texto: "Ingrese un valor válido (0 o mayor)" });
      return;
    }
    setMsg(null);
    setConfirmando(true);
  };

  const handleConfirmar = () => {
    setConfirmando(false);
    mut.mutate();
  };

  return (
    <>
      <div className="flex items-center gap-4 py-3 border-b last:border-0">
        <div className="w-36 shrink-0">
          <div className="font-semibold text-verde-oscuro">{capFirst(insumo.insumo)}</div>
          <div className="text-sm text-gray-500">
            Stock actual: <strong>{formatDecimal(insumo.stockActual)}</strong>
          </div>
        </div>
        <form onSubmit={handleSubmit} className="flex items-center gap-2 flex-1">
          <input
            className="form-control w-36"
            type="number"
            step="0.001"
            min="0"
            placeholder="Nuevo stock"
            value={cantidad}
            onChange={(e) => { setCantidad(e.target.value); setMsg(null); }}
            aria-label={`Nuevo stock de ${insumo.insumo}`}
          />
          <button
            type="submit"
            className="btn btn-primary btn-sm"
            disabled={mut.isPending}
          >
            {mut.isPending ? "Guardando…" : "Ajustar"}
          </button>
          {msg && (
            <span className={`text-sm font-medium ${msg.tipo === "ok" ? "text-verde-claro" : "text-red-600"}`}>
              {msg.texto}
            </span>
          )}
        </form>
      </div>

      {confirmando && (
        <ConfirmDialog
          emoji="📦"
          titulo={`Ajustar stock de ${capFirst(insumo.insumo)}`}
          descripcion={`El stock pasará de ${formatDecimal(insumo.stockActual)} a ${parseFloat(cantidad)}. Este cambio queda registrado en el historial de inventario.`}
          labelOk="Sí, ajustar"
          onOk={handleConfirmar}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </>
  );
}
