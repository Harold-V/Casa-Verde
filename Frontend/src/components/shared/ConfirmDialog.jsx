/**
 * Diálogo de confirmación genérico.
 * z-index 1100 — encima de todos los modales de la app.
 */
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ConfirmDialog({ emoji, titulo, descripcion, labelOk, peligro = false, onOk, onCancelar }) {
  useModalKeyboard({ onEscape: onCancelar, onEnter: onOk });
  return (
    <div className="fixed inset-0 bg-black/60 z-[1100] flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl p-7 max-w-xs w-full text-center animate-slide-up">
        <div className="text-5xl mb-3">{emoji}</div>
        <h3 className="font-bold text-lg mb-1 text-gray-900">{titulo}</h3>
        <p className="text-gray-500 text-sm mb-6">{descripcion}</p>
        <div className="flex gap-3">
          <button className="btn btn-secondary flex-1" onClick={onCancelar}>Volver</button>
          <button className={`btn flex-1 ${peligro ? "btn-danger" : "btn-primary"}`} onClick={onOk}>
            {labelOk}
          </button>
        </div>
      </div>
    </div>
  );
}
