# Electron Auto-Updater — Casa Verde

**Fecha:** 2026-05-29  
**Estado:** Aprobado

---

## Objetivo

Agregar actualizaciones automáticas silenciosas a la app desktop Casa Verde. Cuando el desarrollador publica una nueva versión en GitHub Releases, las instancias instaladas la descargan en background y la instalan automáticamente la próxima vez que el operador cierra la app.

---

## Decisiones de diseño

| Decisión | Elección | Alternativas descartadas |
|---|---|---|
| Proveedor de updates | GitHub Releases (repo privado) | Servidor propio (requiere infra), S3 (costo) |
| UX | Silencioso — descarga en background, instala al cerrar | Notificación con confirmación, manual desde config |
| Token en binario | PAT read-only bakeado en el build | Token en archivo local (requiere provisioning manual) |
| Estrategia de error | Log silencioso, nunca interrumpe operación | Dialog de error (innecesario para app de restaurante) |

---

## Arquitectura

### Flujo de publicación (desarrollador)

```
1. Cambiar versión en Frontend/package.json (ej. 1.0.0 → 1.1.0)
2. GH_TOKEN=<write_token> npm run publish:electron
   └─► electron-builder buildea + sube a GitHub Releases:
         • Casa Verde Setup 1.1.0.exe
         • Casa Verde Setup 1.1.0.exe.blockmap
         • latest.yml
```

### Flujo de actualización (máquina del restaurante)

```
App arranca
  └─► waitForBackend() → createMainWindow()
        └─► setTimeout 3 segundos
              └─► autoUpdater.checkForUpdates()
                    │
                    ├─► Sin versión nueva → nada
                    │
                    ├─► Versión nueva → descarga silenciosa en background
                    │
                    └─► Descarga completa → autoInstallOnAppQuit = true
                          Operador cierra la app
                          └─► NSIS instala la nueva versión
                                Próximo arranque: versión actualizada
```

### Tokens de GitHub

| Token | Scope | Uso | Dónde vive |
|---|---|---|---|
| `GH_TOKEN_WRITE` | `contents: write` | Publicar releases | Solo en la máquina del desarrollador |
| `GH_TOKEN_READ` | `contents: read` | Descargar updates | Bakeado en el binario vía `.env.electron` |

El token READ solo puede leer contenido del repo privado. No puede modificar nada.

---

## Archivos afectados

| Archivo | Acción | Cambio |
|---|---|---|
| `Frontend/package.json` | Modificar | Agregar `electron-updater` en dependencies + sección `publish` en build config + script `publish:electron` |
| `Frontend/electron/main.js` | Modificar | Agregar bloque autoUpdater (~15 líneas) después de `createMainWindow()` en producción |
| `Frontend/resources/update-config.json` | Crear (gitignoreado) | `{ "ghTokenRead": "<token_read_only>" }` |

---

## Implementación detallada

### package.json — sección `build`

```json
"publish": {
  "provider": "github",
  "owner": "Harold-V",
  "repo": "Casa-Verde",
  "private": true
},
"extraResources": [
  { "from": "resources/backend.jar", "to": "backend.jar" },
  { "from": "resources/jre", "to": "jre", "filter": ["**/*"] },
  { "from": "resources/update-config.json", "to": "update-config.json" }
]
```

### package.json — scripts

```json
"publish:electron": "npm run build:react:electron && electron-builder --publish always"
```

### package.json — dependencies

```json
"electron-updater": "^6.6.2"
```

### .env.electron — agregar

```
VITE_GH_TOKEN_READ=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### resources/update-config.json — archivo de configuración del updater

```json
{ "ghTokenRead": "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" }
```

Este archivo vive en `Frontend/resources/update-config.json`, está **gitignoreado**, y se crea manualmente una sola vez en cada máquina de build. `extraResources` lo incluye en el instalador igual que el JAR y el JRE.

`main.js` lo lee en runtime desde `resourcesPath`:

```js
const updateConfigPath = path.join(resourcesPath, "update-config.json");
if (fs.existsSync(updateConfigPath)) {
  const { ghTokenRead } = JSON.parse(fs.readFileSync(updateConfigPath, "utf-8"));
  process.env.GH_TOKEN = ghTokenRead;
}
```

### main.js — bloque autoUpdater

Se agrega en `app.whenReady()`, solo en producción (`!isDev`), después de `createMainWindow()`:

```js
import { autoUpdater } from "electron-updater";

// En producción, después de createMainWindow():
const updateConfigPath = path.join(resourcesPath, "update-config.json");
if (fs.existsSync(updateConfigPath)) {
  const { ghTokenRead } = JSON.parse(fs.readFileSync(updateConfigPath, "utf-8"));
  process.env.GH_TOKEN = ghTokenRead;
}

autoUpdater.autoDownload = true;
autoUpdater.autoInstallOnAppQuit = true;

autoUpdater.on("error", (err) => {
  console.error("[updater] error:", err.message);
});
autoUpdater.on("update-available", (info) => {
  console.log("[updater] nueva versión disponible:", info.version);
});
autoUpdater.on("update-downloaded", (info) => {
  console.log("[updater] versión descargada:", info.version, "— se instalará al cerrar");
});

setTimeout(() => autoUpdater.checkForUpdates(), 3000);
```

---

## Criterios de éxito

- [ ] `npm run publish:electron` sube el instalador a GitHub Releases automáticamente
- [ ] Una instancia instalada detecta la nueva versión en el próximo arranque
- [ ] La descarga ocurre en background sin que el operador note nada
- [ ] Al cerrar la app, NSIS instala la nueva versión silenciosamente
- [ ] Si no hay internet o el token falla, la app arranca normalmente (no crashea)
- [ ] El token READ en el binario no tiene permisos de escritura al repo

---

## Fuera de scope

- UI de progreso de descarga
- Rollback a versión anterior
- Canal beta/estable separados
- Notificación al operador de que hay una actualización disponible
