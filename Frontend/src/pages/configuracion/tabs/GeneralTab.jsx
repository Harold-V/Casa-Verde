import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { configuracionService } from "../../../services/configuracionService";
import { useFormatos } from "../../../hooks/useFormatos";

const CLAVES_EDITABLES = [
  { clave: "nombre_negocio",      label: "Nombre del negocio",              tipo: "text" },
  { clave: "capital_inicial_cop", label: "Capital inicial (COP)",           tipo: "number" },
  { clave: "mostrar_tasa_dolar",  label: "Mostrar tasa USD (1=sí / 0=no)", tipo: "text" },
];

export default function GeneralTab() {
  const qc = useQueryClient();
  const { formatFecha } = useFormatos();
  const [valores, setValores] = useState({});
  const [msg, setMsg] = useState(null);

  const { data: configs = [], isLoading } = useQuery({
    queryKey: ["configuracion"],
    queryFn: configuracionService.listar,
  });

  useEffect(() => {
    const mapa = {};
    configs.forEach((c) => { mapa[c.clave] = c.valor; });
    setValores(mapa);
  }, [configs]);

  const mutGuardar = useMutation({
    mutationFn: ({ clave, valor }) => configuracionService.actualizarValor(clave, valor),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["configuracion"] });
      setMsg({ tipo: "success", texto: "Configuración guardada" });
      setTimeout(() => setMsg(null), 2500);
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const handleGuardar = (clave) => {
    mutGuardar.mutate({ clave, valor: valores[clave] ?? "" });
  };

  if (isLoading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

      <div className="card">
        <div className="card-header"><span className="card-title">Parámetros generales</span></div>
        <div className="card-body">
          {CLAVES_EDITABLES.map(({ clave, label, tipo }) => {
            const config = configs.find((c) => c.clave === clave);
            return (
              <div key={clave} className="form-group">
                <label className="form-label">{label}</label>
                <div className="flex gap-2.5">
                  <input
                    className="form-control"
                    type={tipo}
                    value={valores[clave] ?? ""}
                    onChange={(e) => setValores((v) => ({ ...v, [clave]: e.target.value }))}
                  />
                  <button className="btn btn-primary" onClick={() => handleGuardar(clave)}>
                    Guardar
                  </button>
                </div>
                {config?.actualizadoEn && (
                  <p className="text-[11.5px] text-gray-500 mt-0.5">
                    Actualizado: {formatFecha(config.actualizadoEn)}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </div>

      <div className="card mt-4">
        <div className="card-header"><span className="card-title">Todos los parámetros</span></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Clave</th><th>Valor</th><th>Descripción</th><th>Actualizado</th></tr>
            </thead>
            <tbody>
              {configs.map((c) => (
                <tr key={c.clave}>
                  <td><code className="text-[12px]">{c.clave}</code></td>
                  <td>{c.valor}</td>
                  <td className="text-gray-500">{c.descripcion ?? "—"}</td>
                  <td className="text-gray-500 text-[12px]">{formatFecha(c.actualizadoEn)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
