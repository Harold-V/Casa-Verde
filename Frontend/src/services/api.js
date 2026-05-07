import axios from "axios";

const api = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const mensaje = err.response?.data?.mensaje || err.message || "Error de conexión";
    return Promise.reject(new Error(mensaje));
  }
);

export default api;
