import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { usuarioService } from "../../../services/usuarioService";
import ModalCrearOperador from "../../../components/configuracion/ModalCrearOperador";
import ModalCambiarPassword from "../../../components/configuracion/ModalCambiarPassword";

export default function UsuariosTab() {
  const qc = useQueryClient();
  const [creando, setCreando]                     = useState(false);
  const [cambiandoPassword, setCambiandoPassword] = useState(null);
  const [msg, setMsg]                             = useState(null);

  const { data: usuarios = [], isLoading } = useQuery({
    queryKey: ["usuarios-todos"],
    queryFn:  usuarioService.listarTodos,
  });

  const invalidar = () => qc.invalidateQueries({ queryKey: ["usuarios-todos"] });

  const flash = (tipo, texto) => {
    setMsg({ tipo, texto });
    setTimeout(() => setMsg(null), 2500);
  };

  const mutCrear = useMutation({
    mutationFn: (nombre) => usuarioService.crearOperador(nombre),
    onSuccess: () => { invalidar(); flash("success", "Operador creado"); },
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutToggle = useMutation({
    mutationFn: (id) => usuarioService.toggle(id),
    onSuccess: () => invalidar(),
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutPassword = useMutation({
    mutationFn: ({ id, nuevaPassword }) => usuarioService.cambiarPassword(id, nuevaPassword),
    onSuccess: () => { invalidar(); flash("success", "Contraseña actualizada"); },
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  if (isLoading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo} mb-4`}>{msg.texto}</div>}

      <div className="card">
        <div className="card-header">
          <span className="card-title">Usuarios del sistema</span>
          <button
            className="btn btn-primary btn-sm flex items-center gap-1 ml-auto"
            onClick={() => setCreando(true)}
          >
            <Plus size={14} /> Nuevo operador
          </button>
        </div>
        <div className="table-wrap">
          {usuarios.length === 0 ? (
            <div className="empty-state"><p>Sin usuarios</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map((u) => (
                  <tr key={u.id}>
                    <td className="font-medium">{u.nombre}</td>
                    <td>
                      <span className={`badge ${u.rol === "ADMIN" ? "badge-warning" : "badge-info"}`}>
                        {u.rol}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${u.activo ? "badge-success" : "badge-gray"}`}>
                        {u.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {u.requierePassword && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setCambiandoPassword(u)}
                          >
                            Cambiar clave
                          </button>
                        )}
                        <button
                          className={`btn btn-sm ${u.activo ? "btn-secondary" : "btn-primary"}`}
                          disabled={mutToggle.isPending}
                          onClick={() => mutToggle.mutate(u.id)}
                        >
                          {u.activo ? "Desactivar" : "Activar"}
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

      {creando && (
        <ModalCrearOperador
          onClose={() => setCreando(false)}
          onGuardar={(nombre) => mutCrear.mutateAsync(nombre)}
        />
      )}

      {cambiandoPassword && (
        <ModalCambiarPassword
          usuario={cambiandoPassword}
          onClose={() => setCambiandoPassword(null)}
          onGuardar={(nuevaPassword) =>
            mutPassword.mutateAsync({ id: cambiandoPassword.id, nuevaPassword })
          }
        />
      )}
    </div>
  );
}
