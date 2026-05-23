import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power, Search, X } from "lucide-react";
import { productoService, categoriaService } from "../../../services/productoService";
import { formatCOP } from "../../../utils/format";

function ModalProducto({ producto, categorias, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre: producto?.nombre ?? "",
    precio: producto?.precio ?? 0,
    categoriaId: producto?.categoriaId ?? "",
    afectaInventario: producto?.afectaInventario ?? false,
    composicion: producto?.composicion ?? [],
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const agregarComp = () =>
    setForm((f) => ({ ...f, composicion: [...f.composicion, { insumo: "gallina", cantidad: 1 }] }));

  const quitarComp = (i) =>
    setForm((f) => ({ ...f, composicion: f.composicion.filter((_, idx) => idx !== i) }));

  const setComp = (i, k, v) =>
    setForm((f) => {
      const comp = [...f.composicion];
      comp[i] = { ...comp[i], [k]: v };
      return { ...f, composicion: comp };
    });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onSave({
        ...form,
        precio: parseFloat(form.precio) || 0,
        categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
        composicion: form.afectaInventario ? form.composicion.map((c) => ({
          ...c, cantidad: parseFloat(c.cantidad) || 0,
        })) : [],
      });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">{producto ? "Editar producto" : "Nuevo producto"}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Nombre *</label>
                <input className="form-control" value={form.nombre} onChange={(e) => set("nombre", e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Precio (COP)</label>
                <input className="form-control" type="number" min="0" value={form.precio} onChange={(e) => set("precio", e.target.value)} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Categoría</label>
                <select className="form-control" value={form.categoriaId} onChange={(e) => set("categoriaId", e.target.value)}>
                  <option value="">— Sin categoría —</option>
                  {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                </select>
              </div>
              <div className="form-group flex items-center gap-2.5 pt-[22px]">
                <input type="checkbox" id="afecta" checked={form.afectaInventario} onChange={(e) => set("afectaInventario", e.target.checked)} />
                <label htmlFor="afecta" className="cursor-pointer font-medium">Afecta inventario</label>
              </div>
            </div>

            {form.afectaInventario && (
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="form-label mb-0">Composición de inventario</label>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={agregarComp}>
                    <Plus size={13} /> Agregar insumo
                  </button>
                </div>
                {form.composicion.map((c, i) => (
                  <div key={i} className="composicion-row">
                    <select className="form-control flex-1" value={c.insumo} onChange={(e) => setComp(i, "insumo", e.target.value)}>
                      <option value="gallina">Gallina</option>
                      <option value="cuy">Cuy</option>
                    </select>
                    <input className="form-control w-[100px]" type="number" step="0.001" min="0" value={c.cantidad} onChange={(e) => setComp(i, "cantidad", e.target.value)} />
                    <button type="button" className="btn-icon" onClick={() => quitarComp(i)}><X size={15} /></button>
                  </div>
                ))}
                {form.composicion.length === 0 && (
                  <p className="text-gray-500 text-[13px]">Sin composición — no descontará inventario</p>
                )}
              </div>
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function ProductosTab() {
  const qc = useQueryClient();
  const [busqueda, setBusqueda] = useState("");
  const [soloActivos, setSoloActivos] = useState(true);
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);

  const { data: productos = [], isLoading } = useQuery({
    queryKey: ["productos", soloActivos],
    queryFn: () => productoService.listar(soloActivos),
  });

  const { data: categorias = [] } = useQuery({
    queryKey: ["categorias"],
    queryFn: () => categoriaService.listar(),
  });

  const mutCreate = useMutation({
    mutationFn: productoService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => productoService.actualizar(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutToggle = useMutation({
    mutationFn: productoService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutPrecio = useMutation({
    mutationFn: ({ id, precio }) => productoService.actualizarPrecio(id, precio),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const filtrados = productos.filter((p) =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );

  const handleSave = async (data) => {
    if (editando) {
      await mutUpdate.mutateAsync({ id: editando.id, data });
    } else {
      await mutCreate.mutateAsync(data);
    }
  };

  const handlePrecio = async (p) => {
    const nuevo = window.prompt(`Nuevo precio para "${p.nombre}":`, p.precio);
    if (nuevo === null) return;
    const val = parseFloat(nuevo);
    if (isNaN(val) || val < 0) return;
    mutPrecio.mutate({ id: p.id, precio: val });
  };

  return (
    <div>
      <div className="toolbar">
        <div className="search-wrap">
          <Search size={16} />
          <input className="form-control" placeholder="Buscar producto…" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
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
                        onClick={() => handlePrecio(p)}
                        title="Clic para cambiar precio"
                      >
                        {formatCOP(p.precio)}
                      </span>
                    </td>
                    <td>
                      {p.afectaInventario ? (
                        <span className="badge badge-info">
                          {p.composicion?.map((c) => `${c.cantidad} ${c.insumo}`).join(" + ") || "Sí"}
                        </span>
                      ) : <span className="badge badge-gray">No</span>}
                    </td>
                    <td>
                      <span className={`badge ${p.activo ? "badge-success" : "badge-danger"}`}>
                        {p.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(p)}><Pencil size={15} /></button>
                        <button className="btn-icon" title={p.activo ? "Desactivar" : "Activar"} onClick={() => mutToggle.mutate(p.id)}>
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
    </div>
  );
}
