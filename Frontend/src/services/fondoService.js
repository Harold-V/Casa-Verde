import api from "./api";

export const fondoService = {
  listar: () => api.get("/fondos").then((r) => r.data),
  historial: () => api.get("/fondos/historial").then((r) => r.data),
  ajustar: (moneda, nuevoSaldo, motivo) =>
    api.put(`/fondos/${moneda}`, { nuevoSaldo, motivo }).then((r) => r.data),
};
