import { useState, useMemo } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { proveedorService } from "../../services/proveedorService";
import { compraService } from "../../services/compraService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalRegistrarCompra({ onClose }) {
  const qc = useQueryClient();
  const [form, setForm] = useState({
    proveedorId: "",
    insumo: "gallina",
    cantidad: "",
    precioUnitario: "",
    fecha: hoy(),
    observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: proveedores = [] } = useQuery({
    queryKey: ["proveedores", true],
    queryFn: () => proveedorService.listar(true),
  });

  const total = useMemo(() => {
    const c = parseFloat(form.cantidad);
    const p = parseFloat(form.precioUnitario);
    return !isNaN(c) && !isNaN(p) ? c * p : 0;
  }, [form.cantidad, form.precioUnitario]);

  const mutRegistrar = useMutation({
    mutationFn: compraService.registrar,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["compras"] });
      onClose();
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.proveedorId) { setError("Selecciona un proveedor"); return; }
    if (!form.cantidad || parseFloat(form.cantidad) <= 0) { setError("La cantidad debe ser mayor a 0"); return; }
    if (!form.precioUnitario || parseFloat(form.precioUnitario) <= 0) { setError("El precio debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await mutRegistrar.mutateAsync({
        proveedorId: Number(form.proveedorId),
        insumo: form.insumo,
        cantidad: parseFloat(form.cantidad),
        precioUnitario: parseFloat(form.precioUnitario),
        fecha: form.fecha,
        observacion: form.observacion.trim() || null,
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar compra</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Proveedor *</label>
              <select
                className="form-control"
                value={form.proveedorId}
                onChange={(e) => setForm((f) => ({ ...f, proveedorId: e.target.value }))}
              >
                <option value="">Seleccionar…</option>
                {proveedores.map((p) => (
                  <option key={p.id} value={p.id}>{p.nombre}</option>
                ))}
              </select>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Insumo *</label>
                <select
                  className="form-control"
                  value={form.insumo}
                  onChange={(e) => setForm((f) => ({ ...f, insumo: e.target.value }))}
                >
                  <option value="gallina">Gallina</option>
                  <option value="cuy">Cuy</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Fecha *</label>
                <input
                  type="date"
                  className="form-control"
                  value={form.fecha}
                  onChange={(e) => setForm((f) => ({ ...f, fecha: e.target.value }))}
                />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Cantidad *</label>
                <input
                  type="number"
                  className="form-control"
                  min="0.25"
                  step="0.25"
                  value={form.cantidad}
                  onChange={(e) => setForm((f) => ({ ...f, cantidad: e.target.value }))}
                  placeholder="0"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Precio unitario COP *</label>
                <input
                  type="number"
                  className="form-control"
                  min="1"
                  step="1"
                  value={form.precioUnitario}
                  onChange={(e) => setForm((f) => ({ ...f, precioUnitario: e.target.value }))}
                  placeholder="0"
                />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Observación</label>
              <input
                className="form-control"
                value={form.observacion}
                onChange={(e) => setForm((f) => ({ ...f, observacion: e.target.value }))}
                placeholder="Opcional"
              />
            </div>
            <div className="flex justify-between items-center pt-2 border-t border-gray-100">
              <span className="text-sm text-gray-500">Total estimado</span>
              <span className="font-bold text-lg">{formatCOP(total)}</span>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Registrando…" : "Registrar compra"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
