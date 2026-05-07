import { Menu, DollarSign } from "lucide-react";
import { useSessionStore } from "../../store/useSessionStore";
import { formatCOP } from "../../utils/format";
import { useEffect } from "react";
import { tasaService } from "../../services/tasaService";

export default function Topbar() {
  const toggleSidebar = useSessionStore((s) => s.toggleSidebar);
  const tasaUsd = useSessionStore((s) => s.tasaUsd);
  const setTasaUsd = useSessionStore((s) => s.setTasaUsd);

  useEffect(() => {
    tasaService.vigente()
      .then((t) => setTasaUsd(t.valorCop))
      .catch(() => {});
  }, [setTasaUsd]);

  const fechaHoy = new Date().toLocaleDateString("es-CO", {
    weekday: "short", day: "2-digit", month: "short", year: "numeric",
  });

  return (
    <header className="topbar">
      <button className="topbar-toggle btn-icon" onClick={toggleSidebar}>
        <Menu size={20} />
      </button>
      <span className="topbar-fecha">{fechaHoy}</span>
      <div className="topbar-spacer" />
      {tasaUsd && (
        <div className="topbar-chip">
          <DollarSign size={14} />
          <span>USD = {formatCOP(tasaUsd)}</span>
        </div>
      )}
    </header>
  );
}
