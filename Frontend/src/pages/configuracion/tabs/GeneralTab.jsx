import { useState, useEffect } from "react";
import { useQuery, useQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, X } from "lucide-react";
import { configuracionService } from "../../../services/configuracionService";
import { fondoService } from "../../../services/fondoService";
import { formatCOP, formatFecha, formatUSD } from "../../../utils/format";
import ModalAjuste from "../../../components/fondos/ModalAjuste";

const CLAVES_EDITABLES = [
  { clave: "nombre_negocio",      label: "Nombre del negocio",    tipo: "text" },
  { clave: "capital_inicial_cop", label: "Capital inicial (COP)", tipo: "number" },
  { clave: "zona_horaria",        label: "Zona horaria",          tipo: "text" },
  { clave: "backup_dir",          label: "Directorio de backups", tipo: "text" },
];

export default function GeneralTab() {
  const qc = useQueryClient();
  const [valores, setValores] = useState({});
  const [msg, setMsg] = useState(null);
  const [nuevaAlias, setNuevaAlias] = useState("");
  const [creandoCuenta, setCreandoCuenta] = useState(false);
  const [ajustando, setAjustando] = useState(null);

  const { data: configs = [], isLoading } = useQuery({
    queryKey: ["configuracion"],
    queryFn: configuracionService.listar,
  });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });
  const nequiCuentas = fondos.filter((f) => f.moneda.startsWith("NEQUI"));

  /* ── Estado mensual por cuenta Nequi ───────────────────────────── */
  const estadosNequi = useQueries({
    queries: nequiCuentas.map((f) => ({
      queryKey: ["nequi-estado-mes", f.moneda],
      queryFn: () => fondoService.estadoNequiMes(f.moneda),
    })),
  });

  useEffect(() => {
    const mapa = {};
    configs.forEach((c) => { mapa[c.clave] = c.valor; });
    setValores(mapa);
  }, [configs]);

  const invalidarFondos = () => qc.invalidateQueries({ queryKey: ["fondos"] });

  const flash = (tipo, texto) => {
    setMsg({ tipo, texto });
    setTimeout(() => setMsg(null), 2500);
  };

  const mutCrearNequi = useMutation({
    mutationFn: (alias) => fondoService.crearCuentaNequi(alias),
    onSuccess: () => {
      invalidarFondos();
      setNuevaAlias("");
      setCreandoCuenta(false);
      flash("success", "Cuenta Nequi creada");
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.response?.data?.message ?? err.message }),
  });

  const mutEliminarNequi = useMutation({
    mutationFn: (moneda) => fondoService.eliminarCuentaNequi(moneda),
    onSuccess: () => { invalidarFondos(); flash("success", "Cuenta eliminada"); },
    onError: (err) => setMsg({ tipo: "error", texto: err.response?.data?.message ?? err.message }),
  });

  const ajustarMut = useMutation({
    mutationFn: ({ moneda, nuevoSaldo, motivo }) =>
      fondoService.ajustar(moneda, nuevoSaldo, motivo),
    onSuccess: invalidarFondos,
  });

  const mutGuardar = useMutation({
    mutationFn: ({ clave, valor }) => configuracionService.actualizarValor(clave, valor),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["configuracion"] });
      flash("success", "Configuración guardada");
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const guardar = (clave, valor) => mutGuardar.mutate({ clave, valor: valor ?? valores[clave] ?? "" });

  if (isLoading) return <div className="loading-center"><div className="spinner" /></div>;

  const mostrarTasaBool = valores["mostrar_tasa_dolar"] === "1";

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

      {/* ── Parámetros generales ─────────────────────────────────────── */}
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
                    onKeyDown={(e) => e.key === "Enter" && guardar(clave)}
                  />
                  <button className="btn btn-primary" onClick={() => guardar(clave)}>
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

          {/* mostrar_tasa_dolar — toggle */}
          <div className="form-group mb-0">
            <label className="form-label">Mostrar tasa USD en la barra superior</label>
            <div className="flex items-center gap-3 mt-1">
              <button
                type="button"
                aria-checked={mostrarTasaBool}
                role="switch"
                className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors focus:outline-none ${
                  mostrarTasaBool ? "bg-verde-claro" : "bg-gray-300"
                }`}
                onClick={() => {
                  const nuevo = mostrarTasaBool ? "0" : "1";
                  setValores((v) => ({ ...v, mostrar_tasa_dolar: nuevo }));
                  guardar("mostrar_tasa_dolar", nuevo);
                }}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                    mostrarTasaBool ? "translate-x-6" : "translate-x-1"
                  }`}
                />
              </button>
              <span className="text-[13px] text-gray-700 font-medium">
                {mostrarTasaBool ? "Visible" : "Oculta"}
              </span>
            </div>
            {(() => {
              const config = configs.find((c) => c.clave === "mostrar_tasa_dolar");
              return config?.actualizadoEn
                ? <p className="text-[11.5px] text-gray-500 mt-1">Actualizado: {formatFecha(config.actualizadoEn)}</p>
                : null;
            })()}
          </div>
        </div>
      </div>

      {/* ── Configuración Nequi ──────────────────────────────────────── */}
      <div className="card mt-4">
        <div className="card-header"><span className="card-title">Configuración Nequi</span></div>
        <div className="card-body">

          {/* Cuenta activa */}
          <div className="form-group">
            <label className="form-label">Cuenta activa para pagos y transferencias</label>
            <div className="flex gap-2.5">
              <select
                className="form-control"
                value={valores["cuenta_nequi_activa"] ?? ""}
                onChange={(e) => setValores((v) => ({ ...v, cuenta_nequi_activa: e.target.value }))}
              >
                {nequiCuentas.map((f) => (
                  <option key={f.moneda} value={f.moneda}>
                    {f.moneda}{f.alias ? ` — ${f.alias}` : ""}
                  </option>
                ))}
              </select>
              <button className="btn btn-primary" onClick={() => guardar("cuenta_nequi_activa")}>
                Guardar
              </button>
            </div>
            <p className="text-[11.5px] text-gray-500 mt-1">
              Cuando un pago o cobro usa "NEQUI" sin especificar cuenta, se usa esta.
            </p>
          </div>

          {/* Límite mensual exento */}
          <div className="form-group mb-0">
            <label className="form-label">Límite mensual exento de 4×1000 (COP)</label>
            <div className="flex gap-2.5">
              <input
                className="form-control"
                type="number"
                min="0"
                value={valores["limite_exento_nequi_mensual"] ?? ""}
                onChange={(e) => setValores((v) => ({ ...v, limite_exento_nequi_mensual: e.target.value }))}
                onKeyDown={(e) => e.key === "Enter" && guardar("limite_exento_nequi_mensual")}
              />
              <button className="btn btn-primary" onClick={() => guardar("limite_exento_nequi_mensual")}>
                Guardar
              </button>
            </div>
            <p className="text-[11.5px] text-gray-500 mt-1">
              Retiros hasta este monto/mes están libres de impuesto. Por cuenta individual.
              Reinicia el 1° de cada mes.
            </p>
          </div>
        </div>
      </div>

      {/* ── Cuentas Nequi ───────────────────────────────────────────── */}
      <div className="card mt-4">
        <div className="card-header">
          <span className="card-title">Cuentas Nequi</span>
          <div className="ml-auto">
            {!creandoCuenta ? (
              <button className="btn btn-primary btn-sm flex items-center gap-1"
                onClick={() => setCreandoCuenta(true)}>
                <Plus size={14} /> Nueva cuenta
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <input
                  className="form-control h-8 w-40 text-[13px]"
                  placeholder="Alias (ej. Harold)"
                  autoFocus
                  value={nuevaAlias}
                  onChange={(e) => setNuevaAlias(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && nuevaAlias.trim()) mutCrearNequi.mutate(nuevaAlias.trim());
                    if (e.key === "Escape") { setCreandoCuenta(false); setNuevaAlias(""); }
                  }}
                />
                <button
                  className="btn btn-primary btn-sm"
                  disabled={!nuevaAlias.trim() || mutCrearNequi.isPending}
                  onClick={() => mutCrearNequi.mutate(nuevaAlias.trim())}
                >
                  {mutCrearNequi.isPending ? "Creando…" : "Crear"}
                </button>
                <button className="btn-icon" onClick={() => { setCreandoCuenta(false); setNuevaAlias(""); }}>
                  <X size={16} />
                </button>
              </div>
            )}
          </div>
        </div>
        <div className="table-wrap">
          {nequiCuentas.length === 0 ? (
            <div className="empty-state"><p>Sin cuentas Nequi</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Cuenta</th>
                  <th>Alias</th>
                  <th className="text-right">Saldo</th>
                  <th className="text-right">Usado este mes</th>
                  <th className="text-right">Cupo libre</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {nequiCuentas.map((f, idx) => {
                  const esActiva    = valores["cuenta_nequi_activa"] === f.moneda;
                  const esPrincipal = f.moneda === "NEQUI";
                  const estadoMes   = estadosNequi[idx]?.data;
                  const limiteNum   = estadoMes ? Number(estadoMes.limiteExento) : 0;
                  const usado       = estadoMes ? Number(estadoMes.retiroAcumulado) : null;
                  const libre       = estadoMes ? Number(estadoMes.disponibleLibre)  : null;
                  const agotado     = libre !== null && libre <= 0;

                  return (
                    <tr key={f.moneda}>
                      <td><code className="text-[12px]">{f.moneda}</code></td>
                      <td className="font-medium">{f.alias ?? "—"}</td>
                      <td className="text-right font-semibold">{formatCOP(f.saldo ?? 0)}</td>
                      <td className="text-right text-[12px]">
                        {estadoMes && limiteNum > 0
                          ? <span>{formatCOP(usado)} <span className="text-gray-400">/ {formatCOP(limiteNum)}</span></span>
                          : <span className="text-gray-400">—</span>}
                      </td>
                      <td className="text-right text-[12px]">
                        {estadoMes && limiteNum > 0 ? (
                          <span className={`font-semibold ${agotado ? "text-red-600" : "text-verde-claro"}`}>
                            {formatCOP(Math.max(0, libre))}
                            {agotado && <span className="ml-1 text-[10px] font-normal">(cobra 4×1000)</span>}
                          </span>
                        ) : (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>
                      <td>
                        {esActiva
                          ? <span className="badge badge-success">Activa</span>
                          : <span className="badge badge-gray">—</span>}
                      </td>
                      <td>
                        {!esPrincipal && (
                          <button
                            className="btn-icon text-red-400 hover:text-red-600"
                            title="Eliminar (requiere saldo 0)"
                            disabled={mutEliminarNequi.isPending}
                            onClick={() => {
                              if (window.confirm(`¿Eliminar ${f.moneda} (${f.alias ?? ""})?\nRequiere saldo $0.`)) {
                                mutEliminarNequi.mutate(f.moneda);
                              }
                            }}
                          >
                            <Trash2 size={15} />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* ── Saldos de fondos ─────────────────────────────────────── */}
      <div className="card mt-4">
        <div className="card-header"><span className="card-title">Saldos de fondos</span></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Fondo</th>
                <th className="text-right">Saldo actual</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {fondos.map((f) => {
                const label = f.moneda === "COP"
                  ? "Pesos colombianos"
                  : f.moneda === "USD"
                  ? "Dólares"
                  : `Nequi${f.alias ? ` — ${f.alias}` : ""}`;
                return (
                  <tr key={f.moneda}>
                    <td className="font-medium">{label}</td>
                    <td className="text-right font-semibold">
                      {f.moneda === "USD"
                        ? formatUSD(f.saldo ?? 0)
                        : formatCOP(f.saldo ?? 0)}
                    </td>
                    <td className="text-right">
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => setAjustando(f)}
                      >
                        Ajustar
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Todos los parámetros ─────────────────────────────────────── */}
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

      {ajustando && (
        <ModalAjuste
          fondo={ajustando}
          onClose={() => setAjustando(null)}
          onAjustar={(moneda, nuevoSaldo, motivo) =>
            ajustarMut.mutateAsync({ moneda, nuevoSaldo, motivo })
          }
        />
      )}
    </div>
  );
}
