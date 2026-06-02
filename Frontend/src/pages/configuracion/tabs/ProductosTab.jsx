import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { History, Plus, Pencil, Power, Search } from "lucide-react";
import { productoService, categoriaService } from "../../../services/productoService";
import { formatCOP } from "../../../utils/format";
import { formatFraccion } from "../../../utils/pedidoHelpers";
import ModalProducto from "../../../components/configuracion/ModalProducto";
import ModalHistorialPrecios from "../../../components/configuracion/ModalHistorialPrecios";
import ModalActualizarPrecio from "../../../components/configuracion/ModalActualizarPrecio";

export default function ProductosTab() {
  const qc = useQueryClient();
  const [busqueda, setBusqueda] = useState("");
  const [soloActivos, setSoloActivos] = useState(true);
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);
  const [historialDe, setHistorialDe] = useState(null);
  const [precioModal, setPrecioModal] = useState(null);

  const { data: productos = [], isLoading } = useQuery({
    queryKey: ["productos", soloActivos],
    queryFn: () => productoService.listar(soloActivos),
  });

  const { data: categorias = [] } = useQuery({
    queryKey: ["categorias"],
    queryFn: () => categoriaService.listar(),
  });

  const { data: historialPrecios = [], isLoading: loadingHistorial } = useQuery({
    queryKey: ["producto-precios", historialDe?.id],
    queryFn: () => productoService.historialPrecios(historialDe.id),
    enabled: !!historialDe,
  });

  const mutCreate = useMutation({
    mutationFn: productoService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => productoService.actualizar(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      qc.invalidateQueries({ queryKey: ["pedidos"] });
    },
  });

  const mutToggle = useMutation({
    mutationFn: productoService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutPrecio = useMutation({
    mutationFn: ({ id, precio, motivo }) => productoService.actualizarPrecio(id, precio, motivo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      qc.invalidateQueries({ queryKey: ["pedidos"] });
    },
  });

  const filtrados = productos.filter((p) =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div>
      <div className="toolbar">
        <div className="search-wrap">
          <Search size={16} />
          <input
            className="form-control"
            placeholder="Buscar producto…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
        <label className="flex items-center gap-1.5 text-[13px] cursor-pointer">
          <input type="checkbox" checked={soloActivos} onChange={(e) => setSoloActivos(e.target.checked)} />
          Solo activos
        </label>
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nuevo producto
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : filtrados.length === 0 ? (
            <div className="empty-state"><p>Sin productos</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Categoría</th>
                  <th>Precio</th>
                  <th>Inventario</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filtrados.map((p) => (
                  <tr key={p.id}>
                    <td className="font-medium">{p.nombre}</td>
                    <td className="text-gray-500">{p.categoriaNombre ?? "—"}</td>
                    <td>
                      <span
                        className="cursor-pointer underline decoration-dotted"
                        onClick={() => setPrecioModal(p)}
                        title="Clic para cambiar precio"
                      >
                        {formatCOP(p.precio)}
                      </span>
                    </td>
                    <td>
                      {p.afectaInventario ? (
                        <span className="badge badge-info">
                          {p.composicion?.length > 0
                            ? p.composicion.map((c) => `${formatFraccion(c.cantidad)} ${c.insumo}`).join(" + ")
                            : "Sí"}
                        </span>
                      ) : (
                        <span className="badge badge-gray">No</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${p.activo ? "badge-success" : "badge-danger"}`}>
                        {p.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(p)}>
                          <Pencil size={15} />
                        </button>
                        <button className="btn-icon" title="Historial de precios" onClick={() => setHistorialDe(p)}>
                          <History size={15} />
                        </button>
                        <button
                          className="btn-icon"
                          title={p.activo ? "Desactivar" : "Activar"}
                          onClick={() => mutToggle.mutate(p.id)}
                        >
                          <Power size={15} className={p.activo ? "text-green-600" : "text-red-600"} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {(creando || editando) && (
        <ModalProducto
          producto={editando}
          categorias={categorias}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}

      {historialDe && (
        <ModalHistorialPrecios
          producto={historialDe}
          historial={historialPrecios}
          loading={loadingHistorial}
          onClose={() => setHistorialDe(null)}
        />
      )}

      {precioModal && (
        <ModalActualizarPrecio
          producto={precioModal}
          onClose={() => setPrecioModal(null)}
          onSave={(data) => mutPrecio.mutateAsync(data)}
        />
      )}
    </div>
  );
}
