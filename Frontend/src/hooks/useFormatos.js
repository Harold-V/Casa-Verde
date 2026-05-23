import { formatFecha, formatHora } from "../utils/format";

// Hook simplificado: usa la hora local del sistema operativo directamente
export function useFormatos() {
  return { formatFecha, formatHora };
}
