import api from "./api";

export const proveedorService = {
  listar: (soloActivos = false) =>
    api.get("/proveedores", { params: { soloActivos } }).then((r) => r.data),
  crear: (data) => api.post("/proveedores", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/proveedores/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/proveedores/${id}/toggle`).then((r) => r.data),
};
