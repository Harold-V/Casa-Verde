import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Download, RotateCcw, TrendingUp, TrendingDown,
  DollarSign, SlidersHorizontal, Plus, ArrowUpRight, RefreshCw, Minus, ChevronDown,
} from "lucide-react";
import { fondoService } from "../../services/fondoService";
import { gastoService } from "../../services/gastoService";
import { tasaService } from "../../services/tasaService";
import { exportacionService } from "../../services/exportacionService";
import { formatCOP, formatHora, formatUSD } from "../../utils/format";

const sysTZ = () => { try { return Intl.DateTimeFormat().resolvedOptions().timeZone; } catch { return undefined; } };
const formatFechaFondos = (iso) => {
  if (!iso) return "—";
  const d = new Date(iso);
  const tz = sysTZ();
  const fecha = d.toLocaleDateString("es-CO", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: tz });
  const hora  = d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });
  return `${fecha} · ${hora}`;
};
import ModalEntrada from "../../components/fondos/ModalEntrada";
import ModalTransferencia from "../../components/fondos/ModalTransferencia";
import ModalGasto from "../../components/gastos/ModalGasto";

/* ── helpers visuales ─────────────────────────────────────────────── */
function getFondoLabel(fondo) {
  if (fondo.moneda === "COP")  return "Pesos colombianos";
  if (fondo.moneda === "USD")  return "Dólares estadounidenses";
  if (fondo.moneda.startsWith("NEQUI"))
    return `Nequi${fondo.alias ? ` — ${fondo.alias}` : ""}`;
  return fondo.moneda;
}

function getColorCls(moneda) {
  if (moneda === "COP") return "text-verde-claro";
  if (moneda === "USD") return "text-[#1a6b3c]";
  if (moneda.startsWith("NEQUI")) return "text-[#8c00ff]";
  return "text-verde-oscuro";
}

function getBorderCls(moneda) {
  if (moneda === "COP") return "border-l-verde-claro";
  if (moneda === "USD") return "border-l-[#1a6b3c]";
  if (moneda.startsWith("NEQUI")) return "border-l-[#8c00ff]";
  return "border-l-verde-claro";
}

/** Agrupa todos los movimientos tipo "cobro" por día + moneda en una única fila "Venta del día". */
function procesarHistorial(movs) {
  const cobros = movs.filter((m) => m.tipo === "cobro");
  const otros  = movs.filter((m) => m.tipo !== "cobro");

  const grupos = {};
  cobros.forEach((m) => {
    const dia = m.fecha.substring(0, 10);
    const key = `${dia}__${m.moneda}`;
    if (!grupos[key]) grupos[key] = { dia, moneda: m.moneda, monto: 0, montoCop: 0 };
    grupos[key].monto    += Number(m.monto);
    grupos[key].montoCop += Number(m.montoCop);
  });

  const ventasDia = Object.values(grupos).map((g) => ({
    id:          `venta-${g.dia}-${g.moneda}`,
    tipo:        "cobro",
    concepto:    "Venta del día",
    moneda:      g.moneda,
    monto:       g.monto,
    montoCop:    g.montoCop,
    fecha:       `${g.dia}T23:59:59`,
    _esVentaDia: true,
    _fechaNav:   g.dia,
  }));

  return [...ventasDia, ...otros].sort((a, b) => b.fecha.localeCompare(a.fecha));
}

export default function FondosPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();

  const [fondosExpandidos, setFondosExpandidos]         = useState(false);
  const [modalEntradaOpen, setModalEntradaOpen]         = useState(false);
  const [modalSalidaOpen,  setModalSalidaOpen]          = useState(false);
  const [modalTransferenciaMoneda, setModalTransferenciaMoneda] = useState(null);
  const [filtroFecha, setFiltroFecha]   = useState("");
  const [filtroMoneda, setFiltroMoneda] = useState("");
  const [filtroTipo, setFiltroTipo]     = useState("");

  const { data: fondos = [], isLoading: loadingFondos } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const { data: historial = [], isLoading: loadingHistorial } = useQuery({
    queryKey: ["fondos-historial", filtroFecha, filtroMoneda, filtroTipo],
    queryFn: () => fondoService.historial({
      ...(filtroFecha  ? { fecha: filtroFecha }   : {}),
      ...(filtroMoneda ? { moneda: filtroMoneda } : {}),
      ...(filtroTipo   ? { tipo: filtroTipo }     : {}),
    }),
  });

  const { data: tasa } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
    retry: false,
  });

  const invalidarFondos = () => {
    qc.invalidateQueries({ queryKey: ["fondos"] });
    qc.invalidateQueries({ queryKey: ["fondos-historial"] });
  };

  const gastoMut = useMutation({
    mutationFn: gastoService.registrar,
    onSuccess: () => {
      invalidarFondos();
      qc.invalidateQueries({ queryKey: ["gastos"] });
    },
  });

  const entradaMut = useMutation({
    mutationFn: fondoService.registrarEntrada,
    onSuccess: invalidarFondos,
  });

  const transferenciaMut = useMutation({
    mutationFn: fondoService.registrarTransferencia,
    onSuccess: () => {
      invalidarFondos();
      qc.invalidateQueries({ queryKey: ["nequi-estado-mes"] });
    },
  });

  /* ── cálculos de totales ─────────────────────────────────────────── */
  const saldoCop        = fondos.find((f) => f.moneda === "COP")?.saldo ?? 0;
  const saldoUsd        = fondos.find((f) => f.moneda === "USD")?.saldo ?? 0;
  const nequiFondos     = fondos.filter((f) => f.moneda.startsWith("NEQUI"));
  const saldoNequiTotal = nequiFondos.reduce((acc, f) => acc + Number(f.saldo ?? 0), 0);
  const tasaValor       = tasa?.valorCop ?? null;
  const usdEnCop        = tasaValor != null ? saldoUsd * tasaValor : null;
  const capitalTotal    = tasaValor != null
    ? saldoCop + saldoNequiTotal + usdEnCop
    : saldoCop + saldoNequiTotal;

  const historialProcesado = useMemo(() => procesarHistorial(historial), [historial]);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Fondos</h1>
      </div>

      {/* Card unificada: capital total + fondos individuales */}
      {loadingFondos ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : (
        <div className="card mb-5 overflow-hidden border-l-4 border-l-verde-oscuro">
          {/* Header oscuro — capital total (clickeable para expandir) */}
          <div className="p-[18px] flex justify-between items-center flex-wrap gap-3 cursor-pointer select-none"
            style={{ background: "linear-gradient(135deg, #1a4731 0%, #1a4a2e 100%)" }}
            onClick={() => setFondosExpandidos((v) => !v)}>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <DollarSign size={18} className="text-white/70" />
                <span className="text-[13px] text-white/75 font-medium">Capital neto acumulado</span>
              </div>
              <div className="text-[34px] font-black tracking-tight text-white">
                {formatCOP(capitalTotal)}
              </div>
              <div className="text-[11px] text-white/55 mt-1">
                COP {formatCOP(saldoCop)}
                {" + "}
                NEQUI {formatCOP(saldoNequiTotal)}{nequiFondos.length > 1 ? ` (${nequiFondos.length} cuentas)` : ""}
                {usdEnCop != null
                  ? ` + USD ${formatUSD(saldoUsd)} × ${formatCOP(tasaValor)} = ${formatCOP(usdEnCop)}`
                  : ` + USD ${formatUSD(saldoUsd)} (tasa no configurada)`}
              </div>
            </div>
            <ChevronDown
              size={20}
              className="text-white/60 shrink-0 transition-transform duration-200"
              style={{ transform: fondosExpandidos ? "rotate(180deg)" : "rotate(0deg)" }}
            />
          </div>

          {/* Franja inferior — visible solo cuando está colapsado */}
          {!fondosExpandidos && (
            <div
              className="px-[18px] py-2 flex items-center gap-1.5 cursor-pointer select-none border-t border-white/10"
              style={{ background: "linear-gradient(135deg, #132920 0%, #132920 100%)" }}
              onClick={() => setFondosExpandidos(true)}
            >
              <span className="text-[11px] text-white/50">
                {fondos.map((f) => f.moneda).join(" · ")}
              </span>
              <ChevronDown size={12} className="text-white/40" />
            </div>
          )}

          {/* Fondos individuales — sección blanca con animación */}
          <div
            className="overflow-hidden transition-all duration-300 ease-in-out"
            style={{ maxHeight: fondosExpandidos ? "600px" : "0px" }}
          >
            <div className="p-4 bg-white">
              <div className="grid gap-3" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))" }}>
                {fondos.map((f) => {
                  const esNequi = f.moneda.startsWith("NEQUI");
                  const esUsd   = f.moneda === "USD";
                  return (
                    <div key={f.moneda} className={`border-l-4 ${getBorderCls(f.moneda)} pl-4 py-3 flex flex-col`}>
                      <div className="text-[12px] text-gray-500 mb-1">{getFondoLabel(f)}</div>
                      <div className={`text-[22px] font-extrabold flex-1 ${(f.saldo ?? 0) < 0 ? "text-red-600" : getColorCls(f.moneda)}`}>
                        {f.moneda === "USD"
                          ? formatUSD(f.saldo ?? 0)
                          : formatCOP(f.saldo ?? 0)}
                      </div>
                      <div className="flex items-center justify-between gap-2 mt-2">
                        <div className="text-[11px] text-gray-500 flex flex-col gap-0.5">
                          <span>
                            {f.moneda}
                            {f.moneda === "USD" && tasaValor != null && (
                              <span> · ≈ {formatCOP(Number(f.saldo ?? 0) * tasaValor)} COP</span>
                            )}
                          </span>
                          {f.actualizadoEn && (
                            <span>act. {formatHora(f.actualizadoEn)}</span>
                          )}
                        </div>
                        <div className="h-[28px] flex items-center">
                          {esNequi && (
                            <button
                              className="btn btn-secondary btn-sm flex items-center gap-1"
                              onClick={(e) => { e.stopPropagation(); setModalTransferenciaMoneda(f.moneda); }}
                            >
                              <ArrowUpRight size={13} /> Retiro
                            </button>
                          )}
                          {esUsd && (
                            <button
                              className="btn btn-secondary btn-sm flex items-center gap-1"
                              onClick={(e) => { e.stopPropagation(); setModalTransferenciaMoneda("USD"); }}
                            >
                              <RefreshCw size={13} /> Cambio
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Historial unificado */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Historial de movimientos</span>
          <div className="flex items-center gap-2 ml-auto">
            <button className="btn btn-primary btn-sm flex items-center gap-1" onClick={() => setModalEntradaOpen(true)}>
              <Plus size={14} /> Nueva entrada
            </button>
            <button className="btn btn-danger btn-sm flex items-center gap-1" onClick={() => setModalSalidaOpen(true)}>
              <Minus size={14} /> Nueva salida
            </button>
            <a className="btn btn-secondary btn-sm" href={exportacionService.cajaUrl(filtroFecha)}>
              <Download size={14} />
              CSV caja
            </a>
          </div>
        </div>
        <div className="toolbar px-[18px] pt-4 mb-0">
          <div className="flex items-center gap-1.5 text-[13px] text-gray-500 mr-1">
            <SlidersHorizontal size={15} />
            <span>{historialProcesado.length} registros</span>
          </div>
          <input
            type="date"
            className="form-control w-40"
            value={filtroFecha}
            onChange={(e) => setFiltroFecha(e.target.value)}
          />
          <select className="form-control w-36" value={filtroMoneda} onChange={(e) => setFiltroMoneda(e.target.value)}>
            <option value="">Todas</option>
            <option value="COP">COP</option>
            <option value="USD">USD</option>
            {nequiFondos.map((f) => (
              <option key={f.moneda} value={f.moneda}>
                {f.alias ? `Nequi ${f.alias}` : f.moneda}
              </option>
            ))}
          </select>
          <select className="form-control w-36" value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value)}>
            <option value="">Todo tipo</option>
            <option value="cobro">Ventas</option>
            <option value="entrada">Entradas</option>
            <option value="salida">Salidas</option>
            <option value="ajuste">Ajustes</option>
            <option value="reverso">Reversos</option>
          </select>
          {(filtroFecha || filtroMoneda || filtroTipo) && (
            <button className="btn btn-secondary btn-sm" onClick={() => { setFiltroFecha(""); setFiltroMoneda(""); setFiltroTipo(""); }}>
              Limpiar
            </button>
          )}
        </div>
        <div className="table-wrap">
          {loadingHistorial ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : historialProcesado.length === 0 ? (
            <div className="empty-state"><p>Sin movimientos registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha / Hora</th>
                  <th>Tipo</th>
                  <th>Concepto</th>
                  <th>Moneda</th>
                  <th className="text-right">Monto</th>
                  <th className="text-right">En COP</th>
                </tr>
              </thead>
              <tbody>
                {historialProcesado.map((m) => {
                  const esVentaDia = !!m._esVentaDia;
                  const esEntrada  = esVentaDia || m.tipo === "cobro" || m.tipo === "entrada";
                  const esSalida   = m.tipo === "salida";
                  const esReverso  = m.tipo === "reverso";
                  const esCobro    = esEntrada || esReverso;
                  const Icon  = esEntrada ? TrendingUp : esSalida ? TrendingDown : esReverso ? RotateCcw : DollarSign;
                  const badge = esEntrada || esReverso ? "badge-success" : esSalida ? "badge-danger" : "badge-warning";
                  const color = esEntrada || esReverso ? "text-verde-claro" : esSalida ? "text-red-600" : "text-gray-600";
                  // Etiqueta de moneda: si hay alias, mostrarlo
                  const nequiFondo = fondos.find((f) => f.moneda === m.moneda);
                  const monedaLabel = nequiFondo?.alias && m.moneda.startsWith("NEQUI")
                    ? `Nequi ${nequiFondo.alias}`
                    : m.moneda;
                  return (
                    <tr key={m.id}
                      className={esVentaDia ? "cursor-pointer hover:bg-green-50 transition-colors" : ""}
                      onClick={esVentaDia ? () => navigate("/resumen", { state: { fecha: m._fechaNav } }) : undefined}
                      title={esVentaDia ? "Ver resumen del día" : undefined}
                    >
                      <td className="text-[12px] text-gray-500 whitespace-nowrap">
                        {formatFechaFondos(m.fecha)}
                      </td>
                      <td>
                        <span className={`badge gap-1 ${badge}`}>
                          <Icon size={11} />
                          {m.tipo === "cobro" ? "Entrada" : m.tipo ? m.tipo.charAt(0).toUpperCase() + m.tipo.slice(1) : ""}
                        </span>
                      </td>
                      <td className={esVentaDia ? "font-medium" : ""}>{m.concepto}</td>
                      <td><span className="badge badge-warning">{monedaLabel}</span></td>
                      <td className={`text-right font-semibold ${color}`}>
                        {esCobro ? "+" : "−"}
                        {m.moneda === "USD" ? formatUSD(m.monto) : formatCOP(m.monto)}
                      </td>
                      <td className={`text-right ${color}`}>
                        {esCobro ? "+" : "−"}{formatCOP(m.montoCop)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {modalSalidaOpen && (
        <ModalGasto
          onClose={() => setModalSalidaOpen(false)}
          onGuardar={(data) => gastoMut.mutateAsync(data)}
        />
      )}

      {modalEntradaOpen && (
        <ModalEntrada
          onClose={() => setModalEntradaOpen(false)}
          onGuardar={(data) => entradaMut.mutateAsync(data)}
        />
      )}

      {modalTransferenciaMoneda && (() => {
        const esUsdModal   = modalTransferenciaMoneda === "USD";
        const fondoNequi   = fondos.find((f) => f.moneda === modalTransferenciaMoneda);
        const aliasNequi   = fondoNequi?.alias;
        const tituloModal  = esUsdModal
          ? "Cambio de dólares"
          : `Retiro Nequi${aliasNequi ? ` — ${aliasNequi}` : ""}`;
        const conceptoAuto = esUsdModal
          ? "Cambio USD → COP"
          : `Retiro Nequi${aliasNequi ? ` ${aliasNequi}` : ""}`;
        return (
          <ModalTransferencia
            fondos={fondos}
            monedaOrigenInicial={modalTransferenciaMoneda}
            titulo={tituloModal}
            conceptoFijo={conceptoAuto}
            onClose={() => setModalTransferenciaMoneda(null)}
            onGuardar={(data) => transferenciaMut.mutateAsync(data)}
          />
        );
      })()}
    </div>
  );
}
