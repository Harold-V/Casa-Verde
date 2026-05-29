# Electron Auto-Updater Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar actualizaciones silenciosas automáticas a la app desktop Casa Verde usando `electron-updater` y GitHub Releases privado.

**Architecture:** El token GitHub read-only se guarda en `resources/update-config.json` (gitignoreado), que `main.js` lee en runtime antes de llamar a `autoUpdater`. Las actualizaciones se descargan en background y se instalan silenciosamente cuando el operador cierra la app. Para publicar, el desarrollador usa `GH_TOKEN` (write) via PowerShell + `npm run publish:electron`.

**Tech Stack:** electron-updater 6.6.x, electron-builder 26 (publish provider: github), GitHub Releases API, Node.js fs (ya en main.js).

---

## Mapa de archivos

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `Frontend/.gitignore` | Modificar | Ignorar `resources/update-config.json` |
| `Frontend/resources/update-config.json` | Crear (gitignoreado) | Token GitHub read-only en runtime |
| `Frontend/package.json` | Modificar | Agregar electron-updater + publish config + script |
| `Frontend/electron/main.js` | Modificar | Leer token + configurar autoUpdater + checkForUpdates |

---

## Task 1: Actualizar .gitignore y crear update-config.json

**Files:**
- Modify: `Frontend/.gitignore`
- Create: `Frontend/resources/update-config.json`

- [ ] **Step 1: Agregar update-config.json al .gitignore**

Leer `Frontend/.gitignore` y agregar al final (dentro del bloque `# Recursos bundleados`):

```
resources/update-config.json
```

El bloque completo debe quedar:
```
# Recursos bundleados (generados en build, no van al repo)
resources/backend.jar
resources/jre/
resources/update-config.json
```

- [ ] **Step 2: Crear el archivo update-config.json con token placeholder**

Crear `Frontend/resources/update-config.json`:

```json
{
  "ghTokenRead": "REEMPLAZAR_CON_TOKEN_REAL"
}
```

**Nota:** Este archivo contiene el token real solo en la máquina local. Nunca se commitea. El token se llena en Task 4.

- [ ] **Step 3: Verificar que git ignora el archivo**

```bash
cd Frontend
git status
```

Expected: `resources/update-config.json` NO aparece en el output — git lo ignora correctamente.

- [ ] **Step 4: Commit del .gitignore actualizado**

```bash
git add Frontend/.gitignore
git commit -m "chore(updater): ignorar update-config.json en git

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: Instalar electron-updater y actualizar package.json

**Files:**
- Modify: `Frontend/package.json`

- [ ] **Step 1: Instalar electron-updater**

```bash
cd Frontend
npm install electron-updater
```

Expected: `electron-updater` aparece en `dependencies` de `package.json`.

- [ ] **Step 2: Verificar la versión instalada**

```bash
node -e "const p = require('./node_modules/electron-updater/package.json'); console.log(p.version)"
```

Expected: `6.x.x`

- [ ] **Step 3: Agregar publish config y update-config.json a extraResources en package.json**

Leer `Frontend/package.json`. Reemplazar la sección `"build"` completa con:

```json
"build": {
  "appId": "com.casaverde.casaverde",
  "productName": "Casa Verde",
  "files": [
    "dist/**/*",
    "electron/**/*",
    "package.json"
  ],
  "extraResources": [
    { "from": "resources/backend.jar", "to": "backend.jar" },
    { "from": "resources/jre", "to": "jre", "filter": ["**/*"] },
    { "from": "resources/update-config.json", "to": "update-config.json" }
  ],
  "publish": {
    "provider": "github",
    "owner": "Harold-V",
    "repo": "Casa-Verde",
    "private": true
  },
  "directories": {
    "buildResources": "assets",
    "output": "dist-electron"
  },
  "win": {
    "target": "nsis",
    "forceCodeSigning": false,
    "signtoolOptions": {
      "sign": "./electron/sign.cjs"
    }
  },
  "nsis": {
    "oneClick": false,
    "allowToChangeInstallationDirectory": true,
    "createDesktopShortcut": true,
    "createStartMenuShortcut": true
  }
}
```

- [ ] **Step 4: Agregar script publish:electron a package.json**

En la sección `"scripts"`, agregar después de `"build:electron"`:

```json
"publish:electron": "npm run build:react:electron && electron-builder --publish always",
```

La sección scripts completa queda:
```json
"scripts": {
  "dev": "vite",
  "electron:dev": "concurrently \"npm run dev\" \"electron .\"",
  "build:react": "vite build",
  "build:react:electron": "vite build --mode electron",
  "build:electron": "npm run build:react:electron && electron-builder",
  "publish:electron": "npm run build:react:electron && electron-builder --publish always",
  "build:web": "npm run build:react",
  "electron": "electron .",
  "test": "vitest",
  "test:run": "vitest run",
  "test:coverage": "vitest run --coverage"
},
```

- [ ] **Step 5: Verificar que el JSON es válido**

```bash
cd Frontend
node -e "require('./package.json'); console.log('JSON válido')"
```

Expected: `JSON válido`

- [ ] **Step 6: Commit**

```bash
git add Frontend/package.json Frontend/package-lock.json
git commit -m "feat(updater): instalar electron-updater y configurar publish github

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: Actualizar main.js con el autoUpdater

**Files:**
- Modify: `Frontend/electron/main.js`

**Contexto:** `main.js` ya importa `fs` y tiene `resourcesPath` definido. Solo se agrega el bloque del updater en el lugar correcto: dentro de `app.whenReady()`, en producción (`!isDev`), **después** de `createMainWindow()`. No se modifica ninguna lógica existente.

- [ ] **Step 1: Agregar import de autoUpdater al inicio del archivo**

En `Frontend/electron/main.js`, reemplazar la línea 1:

```js
import { app, BrowserWindow, dialog } from "electron";
```

con:

```js
import { app, BrowserWindow, dialog } from "electron";
import { autoUpdater } from "electron-updater";
```

- [ ] **Step 2: Agregar función setupAutoUpdater**

Agregar esta función después de `waitForBackend` (antes de `app.whenReady()`):

```js
function setupAutoUpdater() {
  const updateConfigPath = path.join(resourcesPath, "update-config.json");
  if (fs.existsSync(updateConfigPath)) {
    try {
      const { ghTokenRead } = JSON.parse(fs.readFileSync(updateConfigPath, "utf-8"));
      if (ghTokenRead && ghTokenRead !== "REEMPLAZAR_CON_TOKEN_REAL") {
        process.env.GH_TOKEN = ghTokenRead;
      }
    } catch (err) {
      console.error("[updater] error leyendo update-config.json:", err.message);
    }
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

  setTimeout(() => {
    autoUpdater.checkForUpdates().catch((err) => {
      console.error("[updater] checkForUpdates falló:", err.message);
    });
  }, 3000);
}
```

- [ ] **Step 3: Llamar setupAutoUpdater después de createMainWindow() en producción**

Localizar el bloque de producción en `app.whenReady()`. Actualmente termina con:

```js
  createMainWindow();
});
```

Reemplazarlo con:

```js
  createMainWindow();
  setupAutoUpdater();
});
```

El `app.whenReady()` completo queda:

```js
app.whenReady().then(async () => {
  ensureDataDirs();

  if (isDev) {
    createMainWindow();
    return;
  }

  createSplashWindow();
  startBackend();

  const ready = await waitForBackend();
  closeSplash();

  if (!ready) {
    dialog.showErrorBox(
      "Casa Verde — Error de inicio",
      "El servidor no pudo iniciar en 30 segundos.\n" +
      "Verificá que no haya otro proceso usando el puerto 5020."
    );
    app.quit();
    return;
  }

  createMainWindow();
  setupAutoUpdater();
});
```

- [ ] **Step 4: Commit**

```bash
git add Frontend/electron/main.js
git commit -m "feat(updater): agregar autoUpdater silencioso con token desde update-config.json

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: Crear GitHub PAT y configurar update-config.json

Este task es **manual** — no lo hace un agente.

- [ ] **Step 1: Crear PAT de lectura (para el instalador)**

1. Ir a https://github.com/settings/tokens/new
2. Nota: `Casa Verde updater - read only`
3. Expiración: 1 año (o sin expiración)
4. Scopes: marcar solo **`Contents` → Read** bajo `Repository permissions` (usar Fine-grained PAT)
   - Si usás Classic token: marcar solo `repo` (read acceso a repos privados)
5. Generar y copiar el token (comienza con `ghp_` o `github_pat_`)

- [ ] **Step 2: Crear PAT de escritura (para publicar releases, solo para el desarrollador)**

1. Ir a https://github.com/settings/tokens/new
2. Nota: `Casa Verde publisher - write`
3. Expiración: 1 año
4. Scopes: **`Contents` → Write** (Fine-grained) o `repo` (Classic)
5. Generar y copiar — guardar en un lugar seguro (Password Manager, etc.)

- [ ] **Step 3: Llenar update-config.json con el token de lectura**

Editar `Frontend/resources/update-config.json`:

```json
{
  "ghTokenRead": "ghp_AQUI_VA_EL_TOKEN_DE_LECTURA_REAL"
}
```

Reemplazar `ghp_AQUI_VA_EL_TOKEN_DE_LECTURA_REAL` con el token generado en Step 1.

- [ ] **Step 4: Verificar que git no lo commitea**

```bash
git status
```

Expected: `resources/update-config.json` NO aparece. Si aparece, revisar que el `.gitignore` tiene la entrada correcta.

---

## Task 5: Build y primera release en GitHub

- [ ] **Step 1: Build del nuevo instalador con update-config.json incluido**

```bash
cd Frontend
npm run build:electron
```

Expected: `dist-electron/Casa Verde Setup 1.0.0.exe` generado sin errores.

- [ ] **Step 2: Verificar que update-config.json quedó en el instalador**

El archivo `update-config.json` debería estar en `dist-electron/win-unpacked/resources/`:

```bash
ls dist-electron/win-unpacked/resources/
```

Expected: aparece `update-config.json` junto a `backend.jar` y `jre/`.

- [ ] **Step 3: Publicar la versión 1.0.0 en GitHub Releases**

Usando el PAT de ESCRITURA generado en Task 4 Step 2:

```powershell
# En PowerShell (no Bash)
$env:GH_TOKEN = "ghp_TOKEN_DE_ESCRITURA_AQUI"
cd Frontend
npm run publish:electron
```

Expected output final:
```
• published      provider=github version=1.0.0
```

Verificar en https://github.com/Harold-V/Casa-Verde/releases que aparece la release `v1.0.0` con los archivos:
- `Casa Verde Setup 1.0.0.exe`
- `Casa Verde Setup 1.0.0.exe.blockmap`
- `latest.yml`

- [ ] **Step 4: Confirmar que latest.yml existe en la release**

`latest.yml` es el archivo que `electron-updater` lee para saber cuál es la versión más nueva. Sin él el auto-updater no funciona.

---

## Task 6: Verificar el flujo completo de actualización

- [ ] **Step 1: Instalar la versión 1.0.0**

Ejecutar `dist-electron/Casa Verde Setup 1.0.0.exe` e instalar.

- [ ] **Step 2: Bumpar la versión a 1.0.1**

En `Frontend/package.json`, cambiar:
```json
"version": "1.0.0"
```
por:
```json
"version": "1.0.1"
```

- [ ] **Step 3: Publicar la versión 1.0.1**

```powershell
$env:GH_TOKEN = "ghp_TOKEN_DE_ESCRITURA_AQUI"
cd Frontend
npm run publish:electron
```

Expected: release `v1.0.1` aparece en GitHub con `latest.yml` actualizado.

- [ ] **Step 4: Abrir la versión 1.0.0 instalada y verificar la descarga**

1. Abrir `Casa Verde` desde el acceso directo del escritorio
2. Esperar ~3 segundos después de que la app cargue
3. Abrir la consola de Electron: menú de la app → Dev Tools, o buscar en logs

Expected en la consola:
```
[updater] nueva versión disponible: 1.0.1
```

Después de unos minutos (descarga en background):
```
[updater] versión descargada: 1.0.1 — se instalará al cerrar
```

- [ ] **Step 5: Cerrar la app y verificar que instala**

Cerrar la ventana de Casa Verde.

Expected: NSIS se ejecuta silenciosamente, instala 1.0.1.

- [ ] **Step 6: Reabrir y verificar la versión**

Abrir Casa Verde nuevamente.

Expected: la ventana About (o el título) muestra la versión 1.0.1, o simplemente la app ya está en 1.0.1 (verificable en `%LOCALAPPDATA%\Programs\Casa Verde\`).

- [ ] **Step 7: Commit de la versión bump**

```bash
git add Frontend/package.json
git commit -m "chore: bump version 1.0.0 → 1.0.1 para test de auto-update

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Workflow de publicación para futuros releases

Cada vez que querés publicar una actualización:

```powershell
# 1. Cambiar version en Frontend/package.json (ej. 1.0.1 → 1.1.0)
# 2. Rebuild del backend si hubo cambios en Java:
cd Backend
.\mvnw.cmd package -DskipTests
Copy-Item "target\backend-0.0.1-SNAPSHOT.jar" "..\Frontend\resources\backend.jar"

# 3. Publicar:
cd ..\Frontend
$env:GH_TOKEN = "ghp_TOKEN_ESCRITURA"
npm run publish:electron

# GitHub Releases recibe el nuevo .exe, latest.yml se actualiza,
# y en el próximo arranque todas las instancias instaladas detectan la versión nueva.
```

---

## Criterios de éxito

- [ ] `git status` no muestra `resources/update-config.json`
- [ ] `npm run build:electron` incluye `update-config.json` en `dist-electron/win-unpacked/resources/`
- [ ] `npm run publish:electron` (con GH_TOKEN write) sube el .exe a GitHub Releases con `latest.yml`
- [ ] La app instalada v1.0.0 loguea `[updater] nueva versión disponible: 1.0.1` al haber una release nueva
- [ ] La app instalada v1.0.0 loguea `[updater] versión descargada: 1.0.1` tras la descarga
- [ ] Cerrar la app instala silenciosamente la nueva versión
- [ ] Si no hay internet, la app arranca sin errores (el `catch` en `checkForUpdates` lo maneja)
