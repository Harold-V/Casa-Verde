import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power, X } from "lucide-react";
import { salonService } from "../../../services/salonService";

function ModalSalon({ salon, onClose, onSave }) {
  const [form, setForm] = useState({ nombre: salon?.nombre ?? "", aforo: salon?.aforo ?? "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onSave({ nombre: form.nombre.trim(), aforo: form.aforo ? Number(form.aforo) : null });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">{salon ? "Editar salón" : "Nuevo salón"}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Nombre *</label>
              <input className="form-control" value={form.nombre} onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="form-label">Aforo (personas)</label>
              <input className="form-control" type="number" min="1" value={form.aforo} onChange={(e) => setForm((f) => ({ ...f, aforo: e.target.value }))} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? "Guardando…" : "Guardar"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function SalonesTab() {
  const qc = useQueryClient();
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);

  const { data: salones = [], isLoading } = useQuery({
    queryKey: ["salones"],
    queryFn: salonService.listar,
  });

  const mutCreate  = useMutation({ mutationFn: salonService.crear,    onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });
  const mutUpdate  = useMutation({ mutationFn: ({ id, data }) => salonService.actualizar(id, data), onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });
  const mutToggle  = useMutation({ mutationFn: salonService.toggle,   onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div>
      <div className="toolbar">
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nuevo salón
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : salones.length === 0 ? (
            <div className="empty-state"><p>Sin salones registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Nombre</th><th>Aforo</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {salones.map((s) => (
                  <tr key={s.id}>
                    <td className="font-medium">{s.nombre}</td>
                    <td>{s.aforo ?? "—"}</td>
                    <td>
                      <span className={`badge ${s.activo ? "badge-success" : "badge-danger"}`}>
                        {s.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" onClick={() => setEditando(s)}><Pencil size={15} /></button>
                        <button className="btn-icon" onClick={() => mutToggle.mutate(s.id)}>
                          <Power size={15} className={s.activo ? "text-green-600" : "text-red-600"} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {(creando || editando) && (
        <ModalSalon
          salon={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
