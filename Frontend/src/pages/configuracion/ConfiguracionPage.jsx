import { useState } from "react";
import ProductosTab from "./tabs/ProductosTab";
import SalonesTab from "./tabs/SalonesTab";
import TasaUsdTab from "./tabs/TasaUsdTab";
import FondosTab from "./tabs/FondosTab";

const TABS = [
  { id: "productos",  label: "Productos" },
  { id: "salones",    label: "Salones" },
  { id: "tasa",       label: "Tasa USD" },
  { id: "fondos",     label: "Fondos" },
];

export default function ConfiguracionPage() {
  const [tab, setTab] = useState("productos");

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Configuración</h1>
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab${tab === t.id ? " activo" : ""}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "productos"  && <ProductosTab />}
      {tab === "salones"    && <SalonesTab />}
      {tab === "tasa"       && <TasaUsdTab />}
      {tab === "fondos"     && <FondosTab />}
    </div>
  );
}
