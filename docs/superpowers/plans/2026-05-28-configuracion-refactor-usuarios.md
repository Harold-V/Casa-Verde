# Configuración Refactor + Pestaña Usuarios Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Limpiar el módulo de Configuración eliminando campos sin valor operativo, mover el toggle de tasa USD, renombrar la pestaña General→Fondos, y agregar la pestaña Usuarios con gestión completa de usuarios del sistema.

**Architecture:** El backend agrega 4 endpoints a `UsuarioController` + lógica en `UsuarioServiceImpl`. El frontend crea `FondosTab.jsx` (refactor de GeneralTab), actualiza `TasaUsdTab`, crea `UsuariosTab` con 2 modales, y actualiza `ConfiguracionPage` con los nuevos tabs.

**Tech Stack:** Spring Boot 3.4 / Java 17 (backend), React 19 + TanStack Query v5 (frontend), BCrypt (ya instalado via spring-security-crypto), Jakarta Validation.

---

## Mapa de archivos

**Backend — modificar:**
- `UsuarioRepository.java` — agregar query method
- `IUsuarioService.java` — agregar 4 métodos
- `UsuarioServiceImpl.java` — implementar 4 métodos
- `UsuarioController.java` — agregar 4 endpoints

**Backend — crear:**
- `CrearOperadorRequest.java`
- `CambiarPasswordRequest.java`

**Frontend — crear:**
- `tabs/FondosTab.jsx` (reemplaza GeneralTab)
- `tabs/UsuariosTab.jsx`
- `components/configuracion/ModalCrearOperador.jsx`
- `components/configuracion/ModalCambiarPassword.jsx`

**Frontend — modificar:**
- `services/usuarioService.js`
- `tabs/TasaUsdTab.jsx`
- `ConfiguracionPage.jsx`

**Frontend — eliminar:**
- `tabs/GeneralTab.jsx`

---

## Task 1: Backend — DTOs, Service, Repository, Controller

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CrearOperadorRequest.java`
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CambiarPasswordRequest.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/UsuarioRepository.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IUsuarioService.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/UsuarioServiceImpl.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaControladores/UsuarioController.java`

- [ ] **Step 1: Crear CrearOperadorRequest.java**

```java
package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CrearOperadorRequest {
    @NotBlank(message = "El nombre del operador es obligatorio")
    private String nombre;
}
```

- [ ] **Step 2: Crear CambiarPasswordRequest.java**

```java
package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CambiarPasswordRequest {
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres")
    private String nuevaPassword;
}
```

- [ ] **Step 3: Agregar query method en UsuarioRepository**

Añadir al final del interface (antes del cierre `}`):
```java
List<Usuario> findAllByOrderByNombreAsc();
```

- [ ] **Step 4: Agregar 4 métodos a IUsuarioService**

```java
List<UsuarioDTO> listarTodos();
UsuarioDTO crearOperador(CrearOperadorRequest request);
UsuarioDTO toggleActivo(Long id);
UsuarioDTO cambiarPassword(Long id, CambiarPasswordRequest request);
```

También agregar import en la interfaz:
```java
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.CambiarPasswordRequest;
```

- [ ] **Step 5: Implementar los 4 métodos en UsuarioServiceImpl**

Añadir los 4 métodos al final de la clase (antes del cierre `}`):

```java
@Transactional(readOnly = true)
public List<UsuarioDTO> listarTodos() {
    return usuarioRepo.findAllByOrderByNombreAsc().stream()
            .map(usuarioMapper::toDTO)
            .toList();
}

@Transactional
public UsuarioDTO crearOperador(CrearOperadorRequest request) {
    if (usuarioRepo.existsByNombreIgnoreCase(request.getNombre())) {
        throw new ValidacionException("Ya existe un usuario con ese nombre");
    }
    Usuario u = Usuario.builder()
            .nombre(request.getNombre())
            .rol(RolUsuario.OPERADOR.name())
            .requierePassword(false)
            .activo(true)
            .build();
    usuarioRepo.save(u);
    auditoriaService.registrar("usuario", u.getId(), "CREAR",
            "Operador creado: " + u.getNombre());
    return usuarioMapper.toDTO(u);
}

@Transactional
public UsuarioDTO toggleActivo(Long id) {
    Usuario u = usuarioRepo.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    if (Boolean.TRUE.equals(u.getActivo()) && RolUsuario.ADMIN.name().equals(u.getRol())) {
        long adminsActivos = usuarioRepo.findAllByOrderByNombreAsc().stream()
                .filter(x -> RolUsuario.ADMIN.name().equals(x.getRol())
                          && Boolean.TRUE.equals(x.getActivo()))
                .count();
        if (adminsActivos <= 1) {
            throw new ValidacionException("No se puede desactivar el único administrador activo");
        }
    }
    u.setActivo(!Boolean.TRUE.equals(u.getActivo()));
    auditoriaService.registrar("usuario", id, "TOGGLE",
            "Usuario " + u.getNombre() + ": activo=" + u.getActivo());
    return usuarioMapper.toDTO(u);
}

@Transactional
public UsuarioDTO cambiarPassword(Long id, CambiarPasswordRequest request) {
    Usuario u = usuarioRepo.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    if (!Boolean.TRUE.equals(u.getRequierePassword())) {
        throw new ValidacionException("Este usuario no maneja contraseña");
    }
    u.setPasswordHash(hashPassword(request.getNuevaPassword()));
    auditoriaService.registrar("usuario", id, "CAMBIAR_PASSWORD",
            "Contraseña actualizada para: " + u.getNombre());
    return usuarioMapper.toDTO(u);
}
```

Agregar imports en UsuarioServiceImpl (si no están):
```java
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.CambiarPasswordRequest;
```

- [ ] **Step 6: Actualizar UsuarioController con 4 endpoints**

Reemplazar el archivo completo con:

```java
package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.CambiarPasswordRequest;
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.LoginRequest;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IUsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UsuarioController {

    private final IUsuarioService service;

    @GetMapping("/api/v1/usuarios")
    public List<UsuarioDTO> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? service.listarTodos() : service.listarActivos();
    }

    @PostMapping("/api/v1/sesion/login")
    public UsuarioDTO login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/api/v1/usuarios")
    public UsuarioDTO crearOperador(@Valid @RequestBody CrearOperadorRequest request) {
        return service.crearOperador(request);
    }

    @PutMapping("/api/v1/usuarios/{id}/toggle")
    public UsuarioDTO toggle(@PathVariable Long id) {
        return service.toggleActivo(id);
    }

    @PutMapping("/api/v1/usuarios/{id}/password")
    public UsuarioDTO cambiarPassword(@PathVariable Long id,
                                      @Valid @RequestBody CambiarPasswordRequest request) {
        return service.cambiarPassword(id, request);
    }
}
```

- [ ] **Step 7: Compilar backend**

```bash
cd Backend && mvn compile -q
```

Salida esperada: BUILD SUCCESS sin errores.

- [ ] **Step 8: Ejecutar tests backend**

```bash
cd Backend && mvn test -q
```

Salida esperada: BUILD SUCCESS, todos los tests pasan.

- [ ] **Step 9: Commit backend**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CrearOperadorRequest.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CambiarPasswordRequest.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/UsuarioRepository.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IUsuarioService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/UsuarioServiceImpl.java \
        Backend/src/main/java/com/casaverde/backend/capaControladores/UsuarioController.java
git commit -m "feat(usuarios): endpoints listarTodos, crearOperador, toggle, cambiarPassword"
```

---

## Task 2: Frontend — usuarioService.js

**Files:**
- Modify: `Frontend/src/services/usuarioService.js`

- [ ] **Step 10: Reemplazar usuarioService.js completo**

```js
import api from "./api";

export const usuarioService = {
  listar: () => api.get("/usuarios").then((r) => r.data),
  listarTodos: () => api.get("/usuarios", { params: { todos: true } }).then((r) => r.data),
  login: (usuarioId, password) =>
    api.post("/sesion/login", { usuarioId, password }).then((r) => r.data),
  crearOperador: (nombre) => api.post("/usuarios", { nombre }).then((r) => r.data),
  toggle: (id) => api.put(`/usuarios/${id}/toggle`).then((r) => r.data),
  cambiarPassword: (id, nuevaPassword) =>
    api.put(`/usuarios/${id}/password`, { nuevaPassword }).then((r) => r.data),
};
```

- [ ] **Step 11: Verificar tests frontend no se rompen**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 12: Commit**

```bash
git add Frontend/src/services/usuarioService.js
git commit -m "feat(usuarios): agregar métodos listarTodos, crearOperador, toggle, cambiarPassword"
```

---

## Task 3: Frontend — FondosTab.jsx (reemplaza GeneralTab.jsx)

**Files:**
- Create: `Frontend/src/pages/configuracion/tabs/FondosTab.jsx`
- Delete: `Frontend/src/pages/configuracion/tabs/GeneralTab.jsx`

- [ ] **Step 13: Crear FondosTab.jsx**

```jsx
import { useState } from "react";
import { useQuery, useQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, X } from "lucide-react";
import { fondoService } from "../../../services/fondoService";
import { formatCOP, formatFecha, formatUSD } from "../../../utils/format";
import ModalAjuste from "../../../components/fondos/ModalAjuste";

export default function FondosTab() {
  const qc = useQueryClient();
  const [nuevaAlias, setNuevaAlias]       = useState("");
  const [creandoCuenta, setCreandoCuenta] = useState(false);
  const [ajustando, setAjustando]         = useState(null);
  const [msg, setMsg]                     = useState(null);

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn:  fondoService.listar,
  });
  const nequiCuentas = fondos.filter((f) => f.moneda.startsWith("NEQUI"));

  const estadosNequi = useQueries({
    queries: nequiCuentas.map((f) => ({
      queryKey: ["nequi-estado-mes", f.moneda],
      queryFn:  () => fondoService.estadoNequiMes(f.moneda),
    })),
  });

  const invalidarFondos = () => qc.invalidateQueries({ queryKey: ["fondos"] });

  const flash = (tipo, texto) => {
    setMsg({ tipo, texto });
    setTimeout(() => setMsg(null), 2500);
  };

  const mutCrearNequi = useMutation({
    mutationFn: (alias) => fondoService.crearCuentaNequi(alias),
    onSuccess: () => {
      invalidarFondos();
      setNuevaAlias("");
      setCreandoCuenta(false);
      flash("success", "Cuenta Nequi creada");
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.response?.data?.message ?? err.message }),
  });

  const mutEliminarNequi = useMutation({
    mutationFn: (moneda) => fondoService.eliminarCuentaNequi(moneda),
    onSuccess: () => { invalidarFondos(); flash("success", "Cuenta eliminada"); },
    onError: (err) => setMsg({ tipo: "error", texto: err.response?.data?.message ?? err.message }),
  });

  const ajustarMut = useMutation({
    mutationFn: ({ moneda, nuevoSaldo, motivo }) =>
      fondoService.ajustar(moneda, nuevoSaldo, motivo),
    onSuccess: invalidarFondos,
  });

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

      {/* ── Configuración Nequi ──────────────────────────────────────── */}
      <div className="card">
        <div className="card-header"><span className="card-title">Configuración Nequi</span></div>
        <div className="card-body">
          <div className="form-group mb-0">
            <label className="form-label">Límite mensual exento de 4×1000 (COP)</label>
            <p className="text-[11.5px] text-gray-500 mt-1">
              Retiros hasta este monto/mes están libres de impuesto. Por cuenta individual. Reinicia el 1° de cada mes.
            </p>
          </div>
        </div>
      </div>

      {/* ── Cuentas Nequi ───────────────────────────────────────────── */}
      <div className="card mt-4">
        <div className="card-header">
          <span className="card-title">Cuentas Nequi</span>
          <div className="ml-auto">
            {!creandoCuenta ? (
              <button className="btn btn-primary btn-sm flex items-center gap-1"
                onClick={() => setCreandoCuenta(true)}>
                <Plus size={14} /> Nueva cuenta
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <input
                  className="form-control h-8 w-40 text-[13px]"
                  placeholder="Alias (ej. Harold)"
                  autoFocus
                  value={nuevaAlias}
                  onChange={(e) => setNuevaAlias(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && nuevaAlias.trim()) mutCrearNequi.mutate(nuevaAlias.trim());
                    if (e.key === "Escape") { setCreandoCuenta(false); setNuevaAlias(""); }
                  }}
                />
                <button
                  className="btn btn-primary btn-sm"
                  disabled={!nuevaAlias.trim() || mutCrearNequi.isPending}
                  onClick={() => mutCrearNequi.mutate(nuevaAlias.trim())}
                >
                  {mutCrearNequi.isPending ? "Creando…" : "Crear"}
                </button>
                <button className="btn-icon" onClick={() => { setCreandoCuenta(false); setNuevaAlias(""); }}>
                  <X size={16} />
                </button>
              </div>
            )}
          </div>
        </div>
        <div className="table-wrap">
          {nequiCuentas.length === 0 ? (
            <div className="empty-state"><p>Sin cuentas Nequi</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Cuenta</th>
                  <th>Alias</th>
                  <th className="text-right">Saldo</th>
                  <th className="text-right">Usado este mes</th>
                  <th className="text-right">Cupo libre</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {nequiCuentas.map((f, idx) => {
                  const estadoMes = estadosNequi[idx]?.data;
                  const limiteNum = estadoMes ? Number(estadoMes.limiteExento) : 0;
                  const usado     = estadoMes ? Number(estadoMes.retiroAcumulado) : null;
                  const libre     = estadoMes ? Number(estadoMes.disponibleLibre)  : null;
                  const agotado   = libre !== null && libre <= 0;
                  const esPrincipal = f.moneda === "NEQUI";

                  return (
                    <tr key={f.moneda}>
                      <td><code className="text-[12px]">{f.moneda}</code></td>
                      <td className="font-medium">{f.alias ?? "—"}</td>
                      <td className="text-right font-semibold">{formatCOP(f.saldo ?? 0)}</td>
                      <td className="text-right text-[12px]">
                        {estadoMes && limiteNum > 0
                          ? <span>{formatCOP(usado)} <span className="text-gray-400">/ {formatCOP(limiteNum)}</span></span>
                          : <span className="text-gray-400">—</span>}
                      </td>
                      <td className="text-right text-[12px]">
                        {estadoMes && limiteNum > 0 ? (
                          <span className={`font-semibold ${agotado ? "text-red-600" : "text-verde-claro"}`}>
                            {formatCOP(Math.max(0, libre))}
                            {agotado && <span className="ml-1 text-[10px] font-normal">(cobra 4×1000)</span>}
                          </span>
                        ) : (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>
                      <td><span className="badge badge-gray">—</span></td>
                      <td>
                        {!esPrincipal && (
                          <button
                            className="btn-icon text-red-400 hover:text-red-600"
                            title="Eliminar (requiere saldo 0)"
                            disabled={mutEliminarNequi.isPending}
                            onClick={() => {
                              if (window.confirm(`¿Eliminar ${f.moneda} (${f.alias ?? ""})?\nRequiere saldo $0.`)) {
                                mutEliminarNequi.mutate(f.moneda);
                              }
                            }}
                          >
                            <Trash2 size={15} />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* ── Saldos de fondos ─────────────────────────────────────── */}
      <div className="card mt-4">
        <div className="card-header"><span className="card-title">Saldos de fondos</span></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Fondo</th>
                <th className="text-right">Saldo actual</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {fondos.map((f) => {
                const label = f.moneda === "COP"
                  ? "Pesos colombianos"
                  : f.moneda === "USD"
                  ? "Dólares"
                  : `Nequi${f.alias ? ` — ${f.alias}` : ""}`;
                return (
                  <tr key={f.moneda}>
                    <td className="font-medium">{label}</td>
                    <td className="text-right font-semibold">
                      {f.moneda === "USD"
                        ? formatUSD(f.saldo ?? 0)
                        : formatCOP(f.saldo ?? 0)}
                    </td>
                    <td className="text-right">
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => setAjustando(f)}
                      >
                        Ajustar
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {ajustando && (
        <ModalAjuste
          fondo={ajustando}
          onClose={() => setAjustando(null)}
          onAjustar={(moneda, nuevoSaldo, motivo) =>
            ajustarMut.mutateAsync({ moneda, nuevoSaldo, motivo })
          }
        />
      )}
    </div>
  );
}
```

- [ ] **Step 14: Eliminar GeneralTab.jsx**

```bash
rm "Frontend/src/pages/configuracion/tabs/GeneralTab.jsx"
```

- [ ] **Step 15: Verificar tests**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 16: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/FondosTab.jsx
git rm Frontend/src/pages/configuracion/tabs/GeneralTab.jsx
git commit -m "refactor(configuracion): GeneralTab → FondosTab, eliminar parámetros sin valor"
```

---

## Task 4: Frontend — TasaUsdTab.jsx (agregar toggle)

**Files:**
- Modify: `Frontend/src/pages/configuracion/tabs/TasaUsdTab.jsx`

- [ ] **Step 17: Reemplazar TasaUsdTab.jsx completo**

```jsx
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { tasaService } from "../../../services/tasaService";
import { configuracionService } from "../../../services/configuracionService";
import { formatCOP, formatFecha } from "../../../utils/format";

export default function TasaUsdTab() {
  const qc = useQueryClient();
  const [nuevaTasa, setNuevaTasa] = useState("");
  const [msg, setMsg] = useState(null);

  const { data: vigente } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
  });
  const { data: historial = [] } = useQuery({
    queryKey: ["tasa-historial"],
    queryFn: tasaService.historial,
  });
  const { data: configs = [] } = useQuery({
    queryKey: ["configuracion"],
    queryFn: configuracionService.listar,
  });

  const mostrarTasa = configs.find((c) => c.clave === "mostrar_tasa_dolar")?.valor === "1";

  const mutRegistrar = useMutation({
    mutationFn: () => tasaService.registrar(parseFloat(nuevaTasa)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasa-vigente"] });
      qc.invalidateQueries({ queryKey: ["tasa-historial"] });
      setNuevaTasa("");
      setMsg({ tipo: "success", texto: "Tasa actualizada correctamente" });
      setTimeout(() => setMsg(null), 3000);
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const mutToggleTasa = useMutation({
    mutationFn: (valor) => configuracionService.actualizarValor("mostrar_tasa_dolar", valor),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["configuracion"] }),
  });

  const handleGuardar = (e) => {
    e.preventDefault();
    const val = parseFloat(nuevaTasa);
    if (!nuevaTasa || isNaN(val) || val <= 0) {
      setMsg({ tipo: "error", texto: "Ingrese un valor válido mayor a 0" });
      return;
    }
    mutRegistrar.mutate();
  };

  return (
    <div>
      <div className="card mb-5">
        <div className="card-header">
          <span className="card-title">Tasa vigente del dólar</span>
        </div>
        <div className="card-body">
          {vigente ? (
            <div className="mb-4">
              <div className="text-[28px] font-bold text-verde-oscuro">
                {formatCOP(vigente.valorCop)}{" "}
                <span className="text-sm text-gray-500 font-normal">COP / USD</span>
              </div>
              <div className="text-gray-500 text-[13px] mt-1">
                Vigente desde: {formatFecha(vigente.vigentDesde)}
              </div>
            </div>
          ) : (
            <div className="alert alert-error">No hay tasa registrada</div>
          )}

          {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

          <form onSubmit={handleGuardar} className="flex gap-2.5 items-end">
            <div className="form-group mb-0 flex-1">
              <label className="form-label">Nueva tasa (COP por 1 USD)</label>
              <input
                className="form-control"
                type="number"
                step="1"
                min="1"
                placeholder="Ej: 4200"
                value={nuevaTasa}
                onChange={(e) => setNuevaTasa(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={mutRegistrar.isPending}>
              {mutRegistrar.isPending ? "Guardando…" : "Guardar tasa"}
            </button>
          </form>
        </div>
      </div>

      <div className="card mb-5">
        <div className="card-header"><span className="card-title">Historial de tasas</span></div>
        <div className="table-wrap">
          {historial.length === 0 ? (
            <div className="empty-state"><p>Sin historial</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Tasa (COP/USD)</th><th>Vigente desde</th></tr>
              </thead>
              <tbody>
                {historial.map((t, i) => (
                  <tr key={t.id}>
                    <td className={i === 0 ? "font-bold" : ""}>{formatCOP(t.valorCop)}</td>
                    <td className="text-gray-500">{formatFecha(t.vigentDesde)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header"><span className="card-title">Visualización</span></div>
        <div className="card-body">
          <div className="form-group mb-0">
            <label className="form-label">Mostrar tasa USD en la barra superior</label>
            <div className="flex items-center gap-3 mt-1">
              <button
                type="button"
                role="switch"
                aria-checked={mostrarTasa}
                className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors focus:outline-none ${
                  mostrarTasa ? "bg-verde-claro" : "bg-gray-300"
                }`}
                onClick={() => mutToggleTasa.mutate(mostrarTasa ? "0" : "1")}
              >
                <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                  mostrarTasa ? "translate-x-6" : "translate-x-1"
                }`} />
              </button>
              <span className="text-[13px] text-gray-700 font-medium">
                {mostrarTasa ? "Visible" : "Oculta"}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 18: Verificar tests**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 19: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/TasaUsdTab.jsx
git commit -m "feat(configuracion): mover toggle tasa USD a pestaña Tasa USD"
```

---

## Task 5: Frontend — Modales de Usuarios

**Files:**
- Create: `Frontend/src/components/configuracion/ModalCrearOperador.jsx`
- Create: `Frontend/src/components/configuracion/ModalCambiarPassword.jsx`

- [ ] **Step 20: Crear ModalCrearOperador.jsx**

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalCrearOperador({ onClose, onGuardar }) {
  const [nombre, setNombre]   = useState("");
  const [error, setError]     = useState("");
  const [saving, setSaving]   = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onGuardar(nombre.trim());
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message ?? "Error al crear");
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[380px]">
        <div className="modal-header">
          <span className="modal-title">Nuevo operador</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group mb-0">
              <label className="form-label">Nombre</label>
              <input
                className="form-control"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="ej. Carlos"
                autoFocus
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Creando…" : "Crear operador"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 21: Crear ModalCambiarPassword.jsx**

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalCambiarPassword({ usuario, onClose, onGuardar }) {
  const [nuevaPassword, setNuevaPassword] = useState("");
  const [error, setError]                 = useState("");
  const [saving, setSaving]               = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nuevaPassword || nuevaPassword.length < 4) {
      setError("La contraseña debe tener al menos 4 caracteres");
      return;
    }
    setSaving(true);
    try {
      await onGuardar(nuevaPassword);
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message ?? "Error al cambiar contraseña");
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[380px]">
        <div className="modal-header">
          <span className="modal-title">Cambiar contraseña — {usuario.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group mb-0">
              <label className="form-label">Nueva contraseña (mínimo 4 caracteres)</label>
              <input
                className="form-control"
                type="password"
                value={nuevaPassword}
                onChange={(e) => setNuevaPassword(e.target.value)}
                autoFocus
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Cambiar contraseña"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 22: Verificar tests**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 23: Commit**

```bash
git add Frontend/src/components/configuracion/ModalCrearOperador.jsx \
        Frontend/src/components/configuracion/ModalCambiarPassword.jsx
git commit -m "feat(usuarios): agregar ModalCrearOperador y ModalCambiarPassword"
```

---

## Task 6: Frontend — UsuariosTab.jsx

**Files:**
- Create: `Frontend/src/pages/configuracion/tabs/UsuariosTab.jsx`

- [ ] **Step 24: Crear UsuariosTab.jsx**

```jsx
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { usuarioService } from "../../../services/usuarioService";
import ModalCrearOperador from "../../../components/configuracion/ModalCrearOperador";
import ModalCambiarPassword from "../../../components/configuracion/ModalCambiarPassword";

export default function UsuariosTab() {
  const qc = useQueryClient();
  const [creando, setCreando]                     = useState(false);
  const [cambiandoPassword, setCambiandoPassword] = useState(null);
  const [msg, setMsg]                             = useState(null);

  const { data: usuarios = [], isLoading } = useQuery({
    queryKey: ["usuarios-todos"],
    queryFn:  usuarioService.listarTodos,
  });

  const invalidar = () => qc.invalidateQueries({ queryKey: ["usuarios-todos"] });

  const flash = (tipo, texto) => {
    setMsg({ tipo, texto });
    setTimeout(() => setMsg(null), 2500);
  };

  const mutCrear = useMutation({
    mutationFn: (nombre) => usuarioService.crearOperador(nombre),
    onSuccess: () => { invalidar(); flash("success", "Operador creado"); },
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutToggle = useMutation({
    mutationFn: (id) => usuarioService.toggle(id),
    onSuccess: () => invalidar(),
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutPassword = useMutation({
    mutationFn: ({ id, nuevaPassword }) => usuarioService.cambiarPassword(id, nuevaPassword),
    onSuccess: () => { invalidar(); flash("success", "Contraseña actualizada"); },
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  if (isLoading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo} mb-4`}>{msg.texto}</div>}

      <div className="card">
        <div className="card-header">
          <span className="card-title">Usuarios del sistema</span>
          <button
            className="btn btn-primary btn-sm flex items-center gap-1 ml-auto"
            onClick={() => setCreando(true)}
          >
            <Plus size={14} /> Nuevo operador
          </button>
        </div>
        <div className="table-wrap">
          {usuarios.length === 0 ? (
            <div className="empty-state"><p>Sin usuarios</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map((u) => (
                  <tr key={u.id}>
                    <td className="font-medium">{u.nombre}</td>
                    <td>
                      <span className={`badge ${u.rol === "ADMIN" ? "badge-warning" : "badge-info"}`}>
                        {u.rol}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${u.activo ? "badge-success" : "badge-gray"}`}>
                        {u.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {u.requierePassword && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setCambiandoPassword(u)}
                          >
                            Cambiar clave
                          </button>
                        )}
                        <button
                          className={`btn btn-sm ${u.activo ? "btn-secondary" : "btn-primary"}`}
                          disabled={mutToggle.isPending}
                          onClick={() => mutToggle.mutate(u.id)}
                        >
                          {u.activo ? "Desactivar" : "Activar"}
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

      {creando && (
        <ModalCrearOperador
          onClose={() => setCreando(false)}
          onGuardar={(nombre) => mutCrear.mutateAsync(nombre)}
        />
      )}

      {cambiandoPassword && (
        <ModalCambiarPassword
          usuario={cambiandoPassword}
          onClose={() => setCambiandoPassword(null)}
          onGuardar={(nuevaPassword) =>
            mutPassword.mutateAsync({ id: cambiandoPassword.id, nuevaPassword })
          }
        />
      )}
    </div>
  );
}
```

- [ ] **Step 25: Verificar tests**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 26: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/UsuariosTab.jsx
git commit -m "feat(usuarios): agregar UsuariosTab con gestión completa de usuarios"
```

---

## Task 7: Frontend — ConfiguracionPage.jsx

**Files:**
- Modify: `Frontend/src/pages/configuracion/ConfiguracionPage.jsx`

- [ ] **Step 27: Reemplazar ConfiguracionPage.jsx completo**

```jsx
import { useState } from "react";
import ProductosTab from "./tabs/ProductosTab";
import SalonesTab   from "./tabs/SalonesTab";
import TasaUsdTab   from "./tabs/TasaUsdTab";
import FondosTab    from "./tabs/FondosTab";
import UsuariosTab  from "./tabs/UsuariosTab";

const TABS = [
  { id: "productos", label: "Productos" },
  { id: "salones",   label: "Salones"   },
  { id: "tasa",      label: "Tasa USD"  },
  { id: "fondos",    label: "Fondos"    },
  { id: "usuarios",  label: "Usuarios"  },
];

export default function ConfiguracionPage() {
  const [tab, setTab] = useState("productos");

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Configuración</h1>
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab${tab === t.id ? " activo" : ""}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "productos" && <ProductosTab />}
      {tab === "salones"   && <SalonesTab />}
      {tab === "tasa"      && <TasaUsdTab />}
      {tab === "fondos"    && <FondosTab />}
      {tab === "usuarios"  && <UsuariosTab />}
    </div>
  );
}
```

- [ ] **Step 28: Verificar tests**

```bash
cd Frontend && npm run test --reporter=verbose
```

Salida esperada: `Tests 100 passed (100)`.

- [ ] **Step 29: Commit**

```bash
git add Frontend/src/pages/configuracion/ConfiguracionPage.jsx
git commit -m "refactor(configuracion): tabs General→Fondos, agregar Usuarios"
```

---

## Task 8: Rebuild y verificación

- [ ] **Step 30: Rebuild completo**

```bash
cd C:/Users/HaroldV/Desktop/Casa-Verde && docker compose up --build -d
```

Esperar hasta que ambos contenedores estén `healthy`.

- [ ] **Step 31: Verificar Configuración > Tasa USD**

Ir a `http://localhost` → Configuración → Tasa USD. Debe verse:
- Tasa vigente
- Historial de tasas
- Card "Visualización" con toggle "Mostrar tasa USD"

- [ ] **Step 32: Verificar Configuración > Fondos**

Ir a Configuración → Fondos. Debe verse:
- Cuentas Nequi
- Saldos de fondos con botones "Ajustar"
- Sin los campos de nombre/zona/backup

- [ ] **Step 33: Verificar Configuración > Usuarios**

1. Ir a Configuración → Usuarios
2. Debe aparecer la tabla con Administrador y Operador
3. Hacer clic en "Cambiar clave" del Administrador → modal de contraseña
4. Hacer clic en "+ Nuevo operador" → modal de nombre
5. Crear un operador llamado "Carlos" → aparece en la tabla
6. Hacer clic en "Desactivar" sobre Carlos → estado cambia a Inactivo
7. Intentar desactivar el Administrador → debe mostrar error "No se puede desactivar el único administrador activo"
