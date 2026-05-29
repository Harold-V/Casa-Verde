# Spec: Refactor Módulo Configuración + Pestaña Usuarios

**Fecha:** 2026-05-28  
**Módulo:** Configuración  
**Estado:** Aprobado

---

## Contexto

El módulo de Configuración es visible solo para el usuario ADMIN. Se detectaron secciones sin valor operativo que se eliminan, y falta la gestión de usuarios (crítica para el arranque inicial del negocio).

---

## Cambios resumen

| Cambio | Descripción |
|---|---|
| Eliminar card "Parámetros generales" de GeneralTab | nombre_negocio, capital_inicial_cop, zona_horaria, backup_dir — no modifican comportamiento |
| Eliminar tabla "Todos los parámetros" de GeneralTab | Solo tiene valor de debug técnico |
| Mover toggle "Mostrar tasa USD" a TasaUsdTab | Más coherente con su contexto |
| Renombrar pestaña "General" → "Fondos" | Solo quedan Nequi + Saldos de fondos |
| Nueva pestaña "Usuarios" | Gestión de usuarios del sistema |
| 3 endpoints nuevos en backend | Listar todos, crear operador, toggle activo, cambiar password |

---

## Backend — Endpoints nuevos

### 1. `GET /api/v1/usuarios?todos=true`
Devuelve todos los usuarios (activos e inactivos) cuando `todos=true`. Sin el parámetro sigue devolviendo solo activos (no rompe el login screen).

### 2. `POST /api/v1/usuarios`
Crea un nuevo operador.  
**Request body** (`CrearOperadorRequest`):
```json
{ "nombre": "Carlos" }
```
- `rol` siempre `OPERADOR`
- `requierePassword` siempre `false`
- `activo` siempre `true`
- Valida: nombre no vacío, nombre único

**Response:** `UsuarioDTO`

### 3. `PUT /api/v1/usuarios/{id}/toggle`
Activa o desactiva un usuario.  
- No se puede desactivar el único ADMIN activo (lanza `ValidacionException`)  
- Response: `UsuarioDTO` actualizado

### 4. `PUT /api/v1/usuarios/{id}/password`
Cambia la contraseña de un usuario con `requierePassword=true`.  
**Request body** (`CambiarPasswordRequest`):
```json
{ "nuevaPassword": "nuevaclave" }
```
- Valida: `nuevaPassword` no vacía, mínimo 4 caracteres
- Hashea con BCrypt
- Solo aplica a usuarios con `requierePassword=true`
- Response: `UsuarioDTO`

### Archivos backend nuevos/modificados
| Archivo | Cambio |
|---|---|
| `fachadaServices/dto/CrearOperadorRequest.java` | Nuevo DTO |
| `fachadaServices/dto/CambiarPasswordRequest.java` | Nuevo DTO |
| `fachadaServices/services/productoService/IUsuarioService.java` | Agregar 3 métodos |
| `fachadaServices/services/productoService/UsuarioServiceImpl.java` | Implementar 3 métodos |
| `capaControladores/UsuarioController.java` | Agregar 3 endpoints |

---

## Frontend — Cambios en archivos existentes

### ConfiguracionPage.jsx
```jsx
const TABS = [
  { id: "productos", label: "Productos" },
  { id: "salones",   label: "Salones"   },
  { id: "tasa",      label: "Tasa USD"  },
  { id: "fondos",    label: "Fondos"    },
  { id: "usuarios",  label: "Usuarios"  },
];
// tab "general" → "fondos"
// agregar import FondosTab y UsuariosTab
```

### GeneralTab.jsx → renombrar a FondosTab.jsx
- Eliminar card "Parámetros generales" (los 4 campos + botones Guardar)
- Eliminar toggle "Mostrar tasa USD" (se mueve a TasaUsdTab)
- Eliminar sección "Todos los parámetros"
- Eliminar imports de `configuracionService`, `useEffect`, `useQueries` si quedan sin uso
- Conservar: Configuración Nequi, Cuentas Nequi, Saldos de fondos

### TasaUsdTab.jsx
Agregar al final de la página (después del historial) la sección del toggle:
```jsx
<div className="card mt-4">
  <div className="card-header"><span className="card-title">Visualización</span></div>
  <div className="card-body">
    <div className="form-group mb-0">
      <label className="form-label">Mostrar tasa USD en la barra superior</label>
      {/* toggle idéntico al que había en GeneralTab */}
    </div>
  </div>
</div>
```
- Necesita: `useQuery(["configuracion"])`, `useMutation` de `configuracionService.actualizarValor`
- El toggle muestra "Visible" / "Oculta" según valor de `mostrar_tasa_dolar`

---

## Frontend — Nuevo archivo UsuariosTab.jsx

**Ruta:** `Frontend/src/pages/configuracion/tabs/UsuariosTab.jsx`

### Layout
```
┌─ Usuarios del sistema ─────────────────────────[+ Nuevo operador]─┐
│  Nombre        Rol        Estado      Acciones                     │
│  Administrador ADMIN      Activo      [Cambiar clave]              │
│  Operador      OPERADOR   Activo      [Desactivar]                 │
│  Carlos        OPERADOR   Inactivo    [Activar]                    │
└────────────────────────────────────────────────────────────────────┘
```

### Datos
- `useQuery(["usuarios-todos"])` → `GET /api/v1/usuarios?todos=true`
- Lista completa (activos + inactivos)

### Acciones por fila
- **Toggle activo/inactivo**: botón `btn-secondary btn-sm` con texto "Desactivar" / "Activar"
- **Cambiar contraseña**: solo visible para usuarios con `requierePassword=true`. Abre `ModalCambiarPassword`

### Botón header
- **"+ Nuevo operador"**: abre `ModalCrearOperador`

### Modales

**ModalCrearOperador**
- Un campo: Nombre (texto, obligatorio)
- Submit: `POST /api/v1/usuarios`
- Al guardar: `invalidateQueries(["usuarios-todos"])`

**ModalCambiarPassword**
- Recibe: `usuario` (objeto)
- Un campo: Nueva contraseña (password, mínimo 4 caracteres)
- Submit: `PUT /api/v1/usuarios/{id}/password`
- Al guardar: `invalidateQueries(["usuarios-todos"])`

### Nuevo servicio
Agregar en `usuarioService.js`:
```js
listarTodos: () => api.get("/usuarios", { params: { todos: true } }).then(r => r.data),
crearOperador: (nombre) => api.post("/usuarios", { nombre }).then(r => r.data),
toggle: (id) => api.put(`/usuarios/${id}/toggle`).then(r => r.data),
cambiarPassword: (id, nuevaPassword) => api.put(`/usuarios/${id}/password`, { nuevaPassword }).then(r => r.data),
```

---

## Archivos frontend nuevos/modificados

| Archivo | Cambio |
|---|---|
| `ConfiguracionPage.jsx` | Actualizar TABS, imports, renders |
| `tabs/GeneralTab.jsx` → `tabs/FondosTab.jsx` | Renombrar + eliminar secciones |
| `tabs/TasaUsdTab.jsx` | Agregar toggle visualización |
| `tabs/UsuariosTab.jsx` | **Nuevo** |
| `components/configuracion/ModalCrearOperador.jsx` | **Nuevo** |
| `components/configuracion/ModalCambiarPassword.jsx` | **Nuevo** |
| `services/usuarioService.js` | Agregar 4 métodos |

---

## Reglas de negocio

- No se puede desactivar el último ADMIN activo (backend lo valida)
- Solo usuarios con `requierePassword=true` pueden cambiar contraseña
- Los operadores no tienen contraseña — no muestran botón "Cambiar clave"
- El login screen sigue usando `listarActivos()` — no se rompe
