import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power } from "lucide-react";
import { salonService } from "../../../services/salonService";
import ModalSalon from "../../../components/configuracion/ModalSalon";

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
