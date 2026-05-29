import { useEffect, useState } from "react";
import { KeyRound, LogIn, UserRound } from "lucide-react";
import { usuarioService } from "../../services/usuarioService";
import { useSessionStore } from "../../store/useSessionStore";

export default function LoginScreen() {
  const setUsuario = useSessionStore((s) => s.setUsuario);
  const [usuarios, setUsuarios] = useState([]);
  const [usuarioId, setUsuarioId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    usuarioService.listar()
      .then((data) => {
        setUsuarios(data);
        if (data.length > 0) setUsuarioId(String(data[0].id));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const usuario = usuarios.find((u) => String(u.id) === String(usuarioId));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      const sesion = await usuarioService.login(Number(usuarioId), password);
      setUsuario(sesion);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-page flex items-center justify-center p-6">
      <div className="card w-full max-w-[420px]">
        <div className="card-header">
          <div>
            <div className="card-title">Casa Verde</div>
            <div className="text-[12px] text-gray-500 mt-1">Selecciona usuario local</div>
          </div>
          <UserRound size={22} className="text-verde-claro" />
        </div>
        <form onSubmit={handleSubmit} className="card-body">
          {error && <div className="alert alert-error">{error}</div>}
          {loading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : (
            <>
              <div className="form-group">
                <label className="form-label">Usuario</label>
                <select
                  className="form-control"
                  value={usuarioId}
                  onChange={(e) => {
                    setUsuarioId(e.target.value);
                    setPassword("");
                  }}
                >
                  {usuarios.map((u) => (
                    <option key={u.id} value={u.id}>{u.nombre} - {u.rol}</option>
                  ))}
                </select>
              </div>

              {usuario?.requierePassword && (
                <div className="form-group">
                  <label className="form-label" htmlFor="password">Clave administrador</label>
                  <div className="relative">
                    <KeyRound size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input
                      id="password"
                      className="form-control pl-9"
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      autoFocus
                    />
                  </div>
                </div>
              )}

              <button className="btn btn-primary w-full justify-center" disabled={saving || !usuarioId}>
                <LogIn size={16} />
                {saving ? "Entrando..." : "Entrar"}
              </button>
            </>
          )}
        </form>
      </div>
    </div>
  );
}
