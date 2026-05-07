import api from "./api";

export const productoService = {
  listar: (soloActivos = false) =>
    api.get("/productos", { params: { soloActivos } }).then((r) => r.data),
  buscar: (id) => api.get(`/productos/${id}`).then((r) => r.data),
  crear: (data) => api.post("/productos", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/productos/${id}`, data).then((r) => r.data),
  actualizarPrecio: (id, precio) =>
    api.put(`/productos/${id}/precio`, { precio }).then((r) => r.data),
  toggle: (id) => api.put(`/productos/${id}/toggle`).then((r) => r.data),
};

export const categoriaService = {
  listar: (soloActivas = false) =>
    api.get("/categorias", { params: { soloActivas } }).then((r) => r.data),
  crear: (data) => api.post("/categorias", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/categorias/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/categorias/${id}/toggle`).then((r) => r.data),
};
