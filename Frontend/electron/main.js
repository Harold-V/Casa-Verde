import { app, BrowserWindow, dialog } from "electron";
import electronUpdater from "electron-updater";
const { autoUpdater } = electronUpdater;
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
  setupAutoUpdater();
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
