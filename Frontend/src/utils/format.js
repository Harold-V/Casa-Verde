export const formatCOP = (valor) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(valor ?? 0);

export const formatFecha = (isoString) => {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("es-CO", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export const formatDecimal = (valor) =>
  typeof valor === "number" ? valor.toFixed(3).replace(/\.?0+$/, "") : "0";
