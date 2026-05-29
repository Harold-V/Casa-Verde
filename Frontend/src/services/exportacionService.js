export const exportacionService = {
  resumenUrl: (fecha) => `/api/v1/exportaciones/resumen${fecha ? `?fecha=${fecha}` : ""}`,
  cajaUrl: (fecha) => `/api/v1/exportaciones/caja${fecha ? `?fecha=${fecha}` : ""}`,
};
