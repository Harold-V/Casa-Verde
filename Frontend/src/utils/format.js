// Lee la zona horaria real del OS vía Intl (funciona incluso en Electron renderer)
const sysTZ = () => {
  try { return Intl.DateTimeFormat().resolvedOptions().timeZone; }
  catch { return undefined; }
};

export const formatFecha = (isoString) => {
  if (!isoString) return "—";
  const d  = new Date(isoString);
  const tz = sysTZ();
  const hora  = d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });
  const fecha = d.toLocaleDateString("es-CO",  { day: "numeric", month: "numeric", year: "numeric", timeZone: tz });
  return `${hora} · ${fecha}`;
};

export const formatHora = (isoString) => {
  if (!isoString) return "—";
  const tz = sysTZ();
  return new Date(isoString).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });
};

export const formatCOP = (valor) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(valor ?? 0);

export const formatDecimal = (valor) =>
  typeof valor === "number" ? valor.toFixed(3).replace(/\.?0+$/, "") : "0";
