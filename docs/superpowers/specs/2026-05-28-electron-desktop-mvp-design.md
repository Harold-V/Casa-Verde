# Electron Desktop MVP — Casa Verde

**Fecha:** 2026-05-28  
**Estado:** Aprobado  

---

## Objetivo

Empaquetar la aplicación Casa Verde como un instalador Windows autónomo (`.exe`) que:
- Incluye el backend Spring Boot (JAR) y un JRE OpenJDK 17
- Incluye el frontend React compilado
- Lanza el backend automáticamente al abrir la app
- Persiste la base de datos SQLite en `%APPDATA%\Casa Verde\`
- Muestra una splash screen mientras el backend inicia

El usuario final no necesita tener Java, Docker, ni ningún conocimiento técnico instalado.

---

## Decisiones de diseño

| Decisión | Elección | Alternativas descartadas |
|---|---|---|
| Distribución del backend | JAR bundleado en extraResources + spawn desde main.js | Docker (requiere Docker Desktop), servicio Windows (más complejo) |
| JRE | Bundleado en el instalador (~180MB) | Asumir Java instalado (falla en máquinas sin Java) |
| Resolución de URL de API | Vite build mode (`.env.electron`) | contextBridge, electron-serve |
| Experiencia de inicio | Splash window hasta que `/actuator/health` responde | App carga directamente (muestra errores momentáneos) |
| Persistencia de datos | `app.getPath('userData')` → `%APPDATA%\Casa Verde\` | Junto al ejecutable (problemas de permisos en Program Files) |
| Plataforma objetivo | Windows (NSIS installer) | Multiplataforma (fuera de scope del MVP) |

---

## Arquitectura

### Flujo de inicio

```
Usuario abre Casa Verde.exe
        │
        ▼
Electron main.js arranca
        │
        ├─► ensureDataDirs()
        │     Crea %APPDATA%\Casa Verde\Database\ y \Backups\ si no existen
        │
        ├─► createSplashWindow()
        │     Ventana 400×300, sin frame, carga electron/splash.html
        │
        ├─► startBackend()   [solo en producción — app.isPackaged]
        │     spawn(jre/bin/java.exe, [
        │       -Dapp.datasource.path=<userData>/Database/casaverde.db,
        │       -Dapp.backup.dir=<userData>/Backups,
        │       -jar, backend.jar
        │     ], { windowsHide: true })
        │
        └─► waitForBackend()
              polling GET http://localhost:5020/actuator/health cada 500ms
              timeout: 30 segundos (60 intentos)
                    │
                    ├─► éxito → closeSplash() → createMainWindow()
                    └─► timeout → closeSplash() → dialog.showErrorBox()
                                                  → app.quit()
```

### Flujo de cierre

```
app.on('will-quit')
  │
  └─► si backendProcess vivo:
        e.preventDefault()
        backendProcess.kill('SIGTERM')
        backendProcess.on('exit') → app.quit()
```

### Resolución de rutas (dev vs producción)

```js
const resourcesPath = app.isPackaged
  ? process.resourcesPath          // <instalaciónDir>/resources/
  : path.join(__dirname, '../resources')  // Frontend/resources/

const javaExe = path.join(resourcesPath, 'jre', 'bin', 'java.exe')
const jarPath = path.join(resourcesPath, 'backend.jar')
```

---

## Archivos afectados

### Nuevos

| Archivo | Descripción |
|---|---|
| `Frontend/electron/splash.html` | Pantalla de carga: logo + spinner + "Iniciando sistema..." |
| `Frontend/.env.electron` | `VITE_API_BASE=http://localhost:5020` |
| `Frontend/resources/backend.jar` | JAR de Spring Boot (gitignoreado, generado en build) |
| `Frontend/resources/jre/` | OpenJDK 17 JRE Windows x64 (gitignoreado, descarga única) |
| `docs/superpowers/specs/2026-05-28-electron-desktop-mvp-design.md` | Este documento |

### Modificados

| Archivo | Cambio |
|---|---|
| `Frontend/electron/main.js` | Reescritura completa: gestión de proceso JVM + splash + health polling |
| `Frontend/package.json` | Nuevos scripts + extraResources en electron-builder config |
| `Frontend/.gitignore` | Agregar `resources/backend.jar`, `resources/jre/`, `dist-electron/` |
| `Frontend/src/services/api.js` | Leer `import.meta.env.VITE_API_BASE` como baseURL de axios |

---

## Configuración electron-builder

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
    "icon": "src/assets/icon.ico",
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

---

## Scripts de package.json

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
  "test:run": "vitest run"
}
```

---

## Comportamiento en desarrollo

Cuando `!app.isPackaged` (modo dev):
- `startBackend()` **no se ejecuta** — el desarrollador corre Spring Boot por separado
- La splash window se cierra inmediatamente sin esperar health check
- La ventana principal carga `http://localhost:5173` (Vite dev server)

Comando de desarrollo desktop:
```bash
# Con backend ya corriendo en :5020
cd Frontend && npm run electron:dev
```

---

## Workflow de build completo

```bash
# Paso 1 — Build backend (repetir en cada cambio de Java)
cd Backend
mvn package -DskipTests
cp target/backend-*.jar ../Frontend/resources/backend.jar

# Paso 2 — JRE (una sola vez)
# 1. Descargar Eclipse Temurin 17 JRE Windows x64 (.zip) desde https://adoptium.net
# 2. Extraer a Frontend/resources/jre/
# 3. Verificar que exista Frontend/resources/jre/bin/java.exe

# Paso 3 — Build instalador
cd Frontend
npm run build:electron
# Salida: Frontend/dist-electron/Casa Verde Setup 1.0.0.exe
```

---

## Persistencia de datos

| Dato | Ruta en Windows |
|---|---|
| Base de datos SQLite | `%APPDATA%\Casa Verde\Database\casaverde.db` |
| Backups de cierre de caja | `%APPDATA%\Casa Verde\Backups\` |

La carpeta se crea automáticamente en el primer arranque. Los datos sobreviven reinstalaciones del instalador.

---

## Criterios de éxito del MVP

- [ ] `npm run build:electron` produce un `.exe` instalable sin errores
- [ ] Al instalar y abrir la app, aparece la splash screen
- [ ] La splash desaparece y la app React carga correctamente (~10s)
- [ ] Login funciona y se pueden crear pedidos
- [ ] Los datos persisten en `%APPDATA%\Casa Verde\` entre reinicios
- [ ] Cerrar la ventana mata el proceso Java (verificable en Task Manager)
- [ ] El build web Docker (`npm run build:react`) sigue funcionando sin cambios

---

## Fuera de scope (MVP)

- Actualizaciones automáticas (`electron-updater`)
- Soporte macOS / Linux
- Icono de sistema en la bandeja (system tray)
- Logs persistentes de errores del backend
- Detección de puerto ocupado / reintentos con puerto alternativo
