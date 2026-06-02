import { useEffect, useState } from "react";
import { Download, LockKeyhole } from "lucide-react";
import { exportacionService } from "../../services/exportacionService";
import ConfirmDialog from "../shared/ConfirmDialog";

/** Panel de cierre de caja diario. Extraído de ResumenPage. */
export default function CierreCajaPanel({ resumen, fecha, cierre, onCerrar, saving, usuario }) {
  const [contadoCop, setContadoCop]     = useState("");
  const [contadoUsd, setContadoUsd]     = useState("");
  const [contadoNequi, setContadoNequi] = useState("");
  const [observacion, setObservacion]   = useState("");
  const [confirmando, setConfirmando]   = useState(false);
  const [datosConfirm, setDatosConfirm] = useState(null);

  useEffect(() => {
    const saldo = (moneda) => resumen.fondos?.find((f) => f.moneda === moneda)?.saldo ?? 0;
    setContadoCop(String(saldo("COP")));
    setContadoUsd(String(saldo("USD")));
    setContadoNequi(String(saldo("NEQUI")));
    setObservacion("");
  }, [fecha, resumen.fondos]);

  const numeroONull = (valor) => (valor === "" || valor == null ? null : Number(valor));
  const bloqueado = resumen.pedidosAbiertos > 0;

  const submit = (e) => {
    e.preventDefault();
    setDatosConfirm({
      fecha,
      usuarioId:    usuario?.id,
      contadoCop:   numeroONull(contadoCop),
      contadoUsd:   numeroONull(contadoUsd),
      contadoNequi: numeroONull(contadoNequi),
      observacion,
    });
    setConfirmando(true);
  };

  return (
    <div className="card mb-6">
      <div className="card-header">
        <div className="flex items-center gap-2">
          <LockKeyhole size={17} className="text-verde-claro" />
          <span className="card-title">Cierre de caja</span>
        </div>
        <a className="btn btn-secondary btn-sm" href={exportacionService.resumenUrl(fecha)}>
          <Download size={14} />
          CSV resumen
        </a>
      </div>
      <div className="card-body">
        {cierre ? (
          <div className="alert alert-success mb-0">
            Caja cerrada por {cierre.usuarioNombre ?? "sistema-local"}.
            {cierre.backupPath && <span> Backup: {cierre.backupPath}</span>}
          </div>
        ) : (
          <form onSubmit={submit}>
            {bloqueado && (
              <div className="alert alert-error">
                Hay pedidos abiertos. Cierra o anula pendientes antes de cerrar caja.
              </div>
            )}
            <div className="form-row-3">
              <div className="form-group">
                <label className="form-label">Contado COP</label>
                <input className="form-control" type="number" step="1" value={contadoCop}
                  onChange={(e) => setContadoCop(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Contado USD</label>
                <input className="form-control" type="number" step="0.01" value={contadoUsd}
                  onChange={(e) => setContadoUsd(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Contado Nequi</label>
                <input className="form-control" type="number" step="1" value={contadoNequi}
                  onChange={(e) => setContadoNequi(e.target.value)} />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Observacion</label>
              <input className="form-control" value={observacion}
                onChange={(e) => setObservacion(e.target.value)} />
            </div>
            <button className="btn btn-primary" disabled={saving || bloqueado}>
              {saving ? "Cerrando..." : "Cerrar caja y crear backup"}
            </button>
          </form>
        )}
      </div>

      {confirmando && (
        <ConfirmDialog
          emoji="🔒"
          titulo="¿Cerrar la caja del día?"
          descripcion="Esta acción es irreversible. Se registrará el cierre, se bloqueará el día y se creará un backup automático."
          labelOk="Sí, cerrar caja"
          onOk={() => { setConfirmando(false); onCerrar(datosConfirm); }}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
