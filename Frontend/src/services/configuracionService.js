import api from "./api";

export const configuracionService = {
  listar: () => api.get("/configuracion").then((r) => r.data),
  actualizarValor: (clave, valor) =>
    api.put(`/configuracion/${clave}`, { valor }).then((r) => r.data),
};
