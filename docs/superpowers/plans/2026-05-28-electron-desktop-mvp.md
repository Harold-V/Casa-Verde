# Electron Desktop MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Empaquetar Casa Verde como instalador Windows autónomo que incluye JRE + JAR, lanza el backend automáticamente, muestra splash screen, y persiste datos en `%APPDATA%\Casa Verde\`.

**Architecture:** Electron main.js spawna el JAR de Spring Boot usando un JRE bundleado en `extraResources`. El frontend React se compila en dos variantes: modo `electron` (baseURL absoluta + hashRouter) y modo `web` (baseURL relativa + browserRouter). Los datos persisten en `app.getPath('userData')`.

**Tech Stack:** Electron 37.2.2, electron-builder 26.0.12, Vite 7 (modes + .env.electron), React Router v7 (createHashRouter), Spring Boot 3.4.7 JAR, OpenJDK 17 JRE Windows x64.

---

## Mapa de archivos

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `Frontend/.gitignore` | Modificar | Excluir resources/jre, resources/backend.jar, dist-electron |
| `Frontend/.env.electron` | Crear | Variables de Vite para build Electron |
| `Frontend/src/services/api.js` | Modificar | Leer VITE_API_BASE como baseURL de axios |
| `Frontend/src/App.jsx` | Modificar | createHashRouter en modo electron, createBrowserRouter en web |
| `Frontend/electron/splash.html` | Crear | Pantalla de carga mientras Spring Boot inicia |
| `Frontend/electron/main.js` | Reescribir | Gestión de proceso JVM, splash, health polling, cleanup |
| `Frontend/package.json` | Modificar | Scripts de build + extraResources en electron-builder |

---

## Task 1: Actualizar .gitignore

**Files:**
- Modify: `Frontend/.gitignore`

- [ ] **Step 1: Agregar entradas al .gitignore de Frontend**

Abrir `Frontend/.gitignore` y agregar al final:

```
# Electron build
dist-electron/

# Recursos bundleados (generados en build, no van al repo)
resources/backend.jar
resources/jre/
```

- [ ] **Step 2: Crear la carpeta resources con un .gitkeep**

```bash
cd Frontend
mkdir resources
echo. > resources/.gitkeep
```

- [ ] **Step 3: Verificar que git no trackea resources/jre y resources/backend.jar**

```bash
git status
```

Expected: `resources/.gitkeep` aparece como untracked, pero si se agrega `resources/backend.jar` luego, git lo ignora.

- [ ] **Step 4: Commit**

```bash
git add Frontend/.gitignore Frontend/resources/.gitkeep
git commit -m "chore(electron): preparar estructura de recursos gitignoreada"
```

---

## Task 2: Crear .env.electron

**Files:**
- Create: `Frontend/.env.electron`

- [ ] **Step 1: Crear el archivo .env.electron**

Crear `Frontend/.env.electron` con exactamente este contenido:

```
VITE_API_BASE=http://localhost:5020/api/v1
VITE_ELECTRON=true
```

`VITE_API_BASE`: reemplaza la baseURL relativa `/api/v1` — necesario porque `file://` no tiene servidor que resuelva rutas relativas.

`VITE_ELECTRON`: señal para que App.jsx use `createHashRouter` — necesario porque `createBrowserRouter` no funciona con protocolo `file://`.

- [ ] **Step 2: Verificar que el archivo existe y tiene el contenido correcto**

```bash
cat Frontend/.env.electron
```

Expected output:
```
VITE_API_BASE=http://localhost:5020/api/v1
VITE_ELECTRON=true
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/.env.electron
git commit -m "chore(electron): agregar variables de entorno para build electron"
```

---

## Task 3: Actualizar api.js para leer VITE_API_BASE

**Files:**
- Modify: `Frontend/src/services/api.js`

**Contexto:** El archivo actual tiene `baseURL: "/api/v1"` hardcodeada. En Electron la app carga desde `file://` — las URLs relativas no se resuelven. `VITE_API_BASE` sobreescribe la baseURL solo cuando se compila con `--mode electron`.

- [ ] **Step 1: Actualizar la baseURL en api.js**

Reemplazar el contenido completo de `Frontend/src/services/api.js`:

```js
import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? "/api/v1",
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
```

- [ ] **Step 2: Verificar que el build web usa la URL relativa**

```bash
cd Frontend
npm run build:react
grep -r "localhost:5020" dist/
```

Expected: grep no encuentra ningún resultado — el build web usa `/api/v1`.

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/services/api.js
git commit -m "feat(electron): leer VITE_API_BASE como baseURL de axios"
```

---

## Task 4: Actualizar App.jsx para usar createHashRouter en Electron

**Files:**
- Modify: `Frontend/src/App.jsx`

**Contexto:** `createBrowserRouter` usa la History API del navegador. Cuando Electron carga `file:///ruta/dist/index.html`, el pathname que ve React Router es la ruta del sistema de archivos, no `/`. Ninguna ruta matchea y se muestra 404. `createHashRouter` usa `#/pedidos` en lugar de `/pedidos`, lo que funciona perfectamente con `file://`.

- [ ] **Step 1: Reemplazar el contenido de App.jsx**

```jsx
import { createBrowserRouter, createHashRouter, RouterProvider } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AppLayout from "./components/layout/AppLayout";
import Dashboard from "./pages/dashboard/Dashboard";
import ConfiguracionPage from "./pages/configuracion/ConfiguracionPage";
import InventarioPage from "./pages/inventario/InventarioPage";
import PedidosPage from "./pages/pedidos/PedidosPage";
import GastosPage from "./pages/gastos/GastosPage";
import ResumenPage from "./pages/resumen/ResumenPage";
import FondosPage from "./pages/fondos/FondosPage";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

function ErrorPage() {
  return (
    <div className="empty-state" style={{ height: "100vh" }}>
      <p className="text-lg font-semibold text-gray-700">Algo salió mal</p>
      <p className="text-sm text-gray-500 mt-1">Recarga la página o contacta al administrador.</p>
      <button className="btn btn-secondary mt-4" onClick={() => window.location.href = "/"}>
        Volver al inicio
      </button>
    </div>
  );
}

function NotFound() {
  return (
    <div className="empty-state" style={{ height: "100vh" }}>
      <p className="text-lg font-semibold text-gray-700">Página no encontrada</p>
      <button className="btn btn-secondary mt-4" onClick={() => window.location.href = "/"}>
        Volver al inicio
      </button>
    </div>
  );
}

const routes = [
  {
    path: "/",
    element: <AppLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true,           element: <Dashboard /> },
      { path: "pedidos",       element: <PedidosPage /> },
      { path: "resumen",       element: <ResumenPage /> },
      { path: "inventario",    element: <InventarioPage /> },
      { path: "gastos",        element: <GastosPage /> },
      { path: "fondos",        element: <FondosPage /> },
      { path: "configuracion", element: <ConfiguracionPage /> },
      { path: "*",             element: <NotFound /> },
    ],
  },
];

// En modo Electron la app corre desde file:// — createBrowserRouter no resuelve
// rutas correctamente. createHashRouter usa /#/ruta que sí funciona con file://.
const router = import.meta.env.VITE_ELECTRON
  ? createHashRouter(routes)
  : createBrowserRouter(routes);

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
```

- [ ] **Step 2: Verificar que el build web sigue usando browserRouter**

```bash
cd Frontend
npm run build:react
```

Expected: build termina sin errores. La app web en Docker no se ve afectada.

- [ ] **Step 3: Verificar que el build electron usa hashRouter**

```bash
npm run build:react:electron
grep -r "createHashRouter\|HashRouter" dist/assets/
```

Expected: el grep encuentra referencias a hash routing en el bundle electron.

- [ ] **Step 4: Commit**

```bash
git add Frontend/src/App.jsx
git commit -m "feat(electron): usar createHashRouter en modo electron para compatibilidad con file://"
```

---

## Task 5: Crear splash.html

**Files:**
- Create: `Frontend/electron/splash.html`

- [ ] **Step 1: Crear el archivo splash.html**

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Casa Verde</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }

    body {
      width: 400px;
      height: 300px;
      background: #1a4731;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-family: system-ui, -apple-system, sans-serif;
      color: #f0faf4;
      user-select: none;
      overflow: hidden;
    }

    .logo {
      font-size: 2.2rem;
      font-weight: 700;
      letter-spacing: -0.5px;
      margin-bottom: 0.25rem;
    }

    .tagline {
      font-size: 0.8rem;
      color: #52b788;
      margin-bottom: 2.5rem;
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }

    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid rgba(255, 255, 255, 0.15);
      border-top-color: #52b788;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin-bottom: 1rem;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .status {
      font-size: 0.75rem;
      color: rgba(255, 255, 255, 0.45);
      letter-spacing: 0.3px;
    }
  </style>
</head>
<body>
  <div class="logo">Casa Verde</div>
  <div class="tagline">Sistema de restaurante</div>
  <div class="spinner"></div>
  <div class="status">Iniciando sistema…</div>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/electron/splash.html
git commit -m "feat(electron): agregar splash screen de inicio"
```

---

## Task 6: Reescribir electron/main.js

**Files:**
- Modify: `Frontend/electron/main.js`

**Contexto:** El `main.js` actual solo crea una ventana. Necesita: (1) spawnear el JAR con JRE bundleado, (2) mostrar splash mientras el backend inicia, (3) hacer polling de `/actuator/health`, (4) matar el proceso Java al cerrar la app. En modo dev (`!app.isPackaged`) NO spawna el backend — el desarrollador lo corre por separado.

- [ ] **Step 1: Reemplazar el contenido completo de main.js**

```js
import { app, BrowserWindow, dialog } from "electron";
import path from "path";
import { fileURLToPath } from "url";
import { spawn } from "child_process";
import http from "http";
import fs from "fs";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Propagar timezone del OS al renderer (Electron usa UTC por defecto)
process.env.TZ = Intl.DateTimeFormat().resolvedOptions().timeZone;

const isDev = !app.isPackaged;

// En dev apunta a Frontend/resources/; en producción a resources/ junto al asar
const resourcesPath = isDev
  ? path.join(__dirname, "../resources")
  : process.resourcesPath;

let backendProcess = null;
let splashWindow = null;
let mainWindow = null;

function ensureDataDirs() {
  const userData = app.getPath("userData");
  fs.mkdirSync(path.join(userData, "Database"), { recursive: true });
  fs.mkdirSync(path.join(userData, "Backups"), { recursive: true });
}

function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 400,
    height: 300,
    frame: false,
    resizable: false,
    center: true,
    alwaysOnTop: true,
    webPreferences: { contextIsolation: true },
  });
  splashWindow.loadFile(path.join(__dirname, "splash.html"));
}

function closeSplash() {
  if (splashWindow && !splashWindow.isDestroyed()) {
    splashWindow.close();
    splashWindow = null;
  }
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  if (isDev) {
    mainWindow.loadURL("http://localhost:5173");
  } else {
    mainWindow.loadFile(path.join(__dirname, "../dist/index.html"));
  }

  mainWindow.once("ready-to-show", () => mainWindow.show());
}

function startBackend() {
  const userData = app.getPath("userData");
  const javaExe = path.join(resourcesPath, "jre", "bin", "java.exe");
  const jarPath = path.join(resourcesPath, "backend.jar");
  const dbPath = path.join(userData, "Database", "casaverde.db");
  const backupDir = path.join(userData, "Backups");

  backendProcess = spawn(
    javaExe,
    [
      `-Dapp.datasource.path=${dbPath}`,
      `-Dapp.backup.dir=${backupDir}`,
      "-jar",
      jarPath,
    ],
    { windowsHide: true, detached: false }
  );

  backendProcess.stdout.on("data", (d) =>
    console.log("[backend]", d.toString().trim())
  );
  backendProcess.stderr.on("data", (d) =>
    console.error("[backend]", d.toString().trim())
  );
  backendProcess.on("exit", (code) =>
    console.log(`[backend] proceso terminó con código ${code}`)
  );
}

function checkHealth() {
  return new Promise((resolve, reject) => {
    const req = http.get("http://localhost:5020/actuator/health", (res) => {
      if (res.statusCode === 200) resolve();
      else reject(new Error(`HTTP ${res.statusCode}`));
      res.resume();
    });
    req.on("error", reject);
    req.setTimeout(1000, () => {
      req.destroy();
      reject(new Error("timeout"));
    });
  });
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForBackend(retries = 60, delayMs = 500) {
  for (let i = 0; i < retries; i++) {
    try {
      await checkHealth();
      return true;
    } catch {
      await sleep(delayMs);
    }
  }
  return false;
}

app.whenReady().then(async () => {
  ensureDataDirs();

  if (isDev) {
    // Modo desarrollo: el desarrollador corre Spring Boot por separado
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
});

app.on("will-quit", (e) => {
  if (backendProcess && !backendProcess.killed) {
    e.preventDefault();
    backendProcess.kill();
    backendProcess.once("exit", () => app.quit());
  }
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/electron/main.js
git commit -m "feat(electron): reescribir main.js con gestión de proceso JVM y splash screen"
```

---

## Task 7: Actualizar package.json

**Files:**
- Modify: `Frontend/package.json`

**Contexto:** Necesitamos (1) nuevos scripts de build separados para web y electron, (2) `extraResources` para que electron-builder copie el JAR y el JRE al instalador, (3) config NSIS con accesos directos, (4) quitar la referencia al ícono inexistente para evitar que el build falle.

- [ ] **Step 1: Actualizar scripts y configuración de build en package.json**

Reemplazar las secciones `"scripts"` y `"build"` con:

```json
  "scripts": {
    "dev": "vite",
    "electron:dev": "concurrently \"npm run dev\" \"electron .\"",
    "build:react": "vite build",
    "build:react:electron": "vite build --mode electron",
    "build:electron": "npm run build:react:electron && electron-builder",
    "build:web": "npm run build:react",
    "electron": "electron .",
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  },
```

Y la sección `"build"`:

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
      { "from": "resources/jre", "to": "jre", "filter": ["**/*"] }
    ],
    "directories": {
      "buildResources": "assets",
      "output": "dist-electron"
    },
    "win": {
      "target": "nsis",
      "forceCodeSigning": false
    },
    "nsis": {
      "oneClick": false,
      "allowToChangeInstallationDirectory": true,
      "createDesktopShortcut": true,
      "createStartMenuShortcut": true
    }
  }
```

Nota: se eliminó `"icon": "src/assets/icon.ico"` porque el archivo no existe. electron-builder usará el ícono por defecto de Electron. Se puede agregar un ícono más adelante.

- [ ] **Step 2: Verificar que el package.json es JSON válido**

```bash
cd Frontend
node -e "require('./package.json'); console.log('JSON válido')"
```

Expected: `JSON válido`

- [ ] **Step 3: Commit**

```bash
git add Frontend/package.json
git commit -m "feat(electron): actualizar scripts y configuración electron-builder con extraResources"
```

---

## Task 8: Verificar que el build web sigue funcionando

**Files:** ninguno (solo verificación)

Este task confirma que los cambios no rompieron el build Docker/web.

- [ ] **Step 1: Ejecutar build web**

```bash
cd Frontend
npm run build:react
```

Expected: build termina sin errores, crea `Frontend/dist/`.

- [ ] **Step 2: Confirmar que api.js en el build web usa URL relativa**

```bash
grep -r "localhost:5020" dist/assets/*.js
```

Expected: no hay resultados — el build web usa `/api/v1`.

- [ ] **Step 3: Confirmar que App.jsx en el build web usa browserRouter**

```bash
grep -r "createBrowserRouter\|__VITE_ELECTRON__\|hash" dist/assets/*.js | head -5
```

Expected: no se encuentra `hash` en el contexto del router — usa browserRouter.

---

## Task 9: Prueba en modo desarrollo desktop

**Prerequisito:** Spring Boot corriendo en `localhost:5020` (vía Docker Compose o `mvn spring-boot:run`).

- [ ] **Step 1: Asegurarse que el backend está corriendo**

```bash
curl http://localhost:5020/actuator/health
```

Expected: `{"status":"UP"}`

- [ ] **Step 2: Lanzar Electron en modo dev**

```bash
cd Frontend
npm run electron:dev
```

Expected:
- Vite inicia en `:5173`
- Se abre una ventana Electron cargando `http://localhost:5173`
- La app React aparece con el login screen
- En la consola de Electron no hay errores de CORS ni 404

- [ ] **Step 3: Verificar navegación entre rutas**

Iniciar sesión y navegar a Pedidos, Inventario, Fondos.

Expected: la navegación funciona correctamente, sin 404.

- [ ] **Step 4: Cerrar la ventana Electron**

Expected: el proceso Electron termina. El proceso Spring Boot sigue corriendo (en dev mode, Electron no lo spawneó ni lo mata).

---

## Task 10: Setup de recursos y build del instalador

**Prerequisito:** JRE descargado y copiado (ver Step 1). Backend compilado (ver Step 2).

- [ ] **Step 1: Descargar JRE OpenJDK 17 para Windows x64**

1. Ir a https://adoptium.net/temurin/releases/?version=17
2. Filtrar: Operating System = Windows, Architecture = x64, Package Type = **JRE**, Version = 17
3. Descargar el archivo `.zip` (no el `.msi`)
4. Extraer el contenido del zip en `Frontend/resources/jre/`
5. Verificar que existe el ejecutable:

```bash
Frontend/resources/jre/bin/java.exe --version
```

Expected: `openjdk 17.x.x ...`

- [ ] **Step 2: Compilar el backend y copiar el JAR**

```bash
cd Backend
mvn package -DskipTests
```

Expected: `BUILD SUCCESS`, archivo creado en `Backend/target/backend-*.jar`

```bash
# Desde la raíz del proyecto
cp Backend/target/backend-*.jar Frontend/resources/backend.jar
```

- [ ] **Step 3: Verificar que los recursos están en su lugar**

```bash
ls Frontend/resources/
```

Expected:
```
backend.jar
jre/
.gitkeep
```

```bash
ls Frontend/resources/jre/bin/java.exe
```

Expected: el archivo existe.

- [ ] **Step 4: Ejecutar el build del instalador**

```bash
cd Frontend
npm run build:electron
```

Este comando ejecuta `vite build --mode electron` + `electron-builder`. Tarda varios minutos (empaqueta todo).

Expected output final:
```
  • building        target=NSIS name="Casa Verde" version=1.0.0
  • built           path=dist-electron/Casa Verde Setup 1.0.0.exe
```

- [ ] **Step 5: Instalar y probar el instalador**

1. Ejecutar `Frontend/dist-electron/Casa Verde Setup 1.0.0.exe`
2. Seguir el wizard de instalación
3. Abrir la app desde el acceso directo en el escritorio

Expected:
- Aparece la splash screen "Casa Verde" con spinner verde sobre fondo verde oscuro
- Después de ~10 segundos, la splash desaparece y carga la app React
- La pantalla de login aparece correctamente
- Iniciar sesión y navegar a Pedidos funciona

- [ ] **Step 6: Verificar persistencia de datos**

1. Abrir el Explorador de Windows y navegar a `%APPDATA%\Casa Verde\`
2. Verificar que existen las carpetas `Database\` y `Backups\`
3. Verificar que existe `Database\casaverde.db`

- [ ] **Step 7: Verificar que cerrar la app mata el proceso Java**

1. Abrir Task Manager (Ctrl+Shift+Esc)
2. Buscar un proceso `java.exe` mientras la app está abierta — debe existir
3. Cerrar la ventana de Casa Verde
4. Verificar en Task Manager que `java.exe` desapareció

- [ ] **Step 8: Commit final**

```bash
git add Frontend/resources/.gitkeep
git commit -m "feat(electron): MVP desktop listo — instalador Windows autónomo con JRE bundleado"
```

---

## Criterios de éxito

- [ ] `npm run build:react` produce build web sin `localhost:5020` hardcodeado
- [ ] `npm run build:electron` produce `dist-electron/Casa Verde Setup 1.0.0.exe` sin errores
- [ ] El instalador corre en Windows sin necesitar Java instalado
- [ ] La splash screen aparece al abrir la app
- [ ] La app React carga correctamente (~10s) y el login funciona
- [ ] La navegación entre módulos funciona (sin 404 por hashRouter)
- [ ] Los datos persisten en `%APPDATA%\Casa Verde\` entre reinicios
- [ ] Cerrar la app termina el proceso `java.exe`
- [ ] El build Docker/web (`npm run build:react`) sigue funcionando sin cambios
