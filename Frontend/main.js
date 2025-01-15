const { app, BrowserWindow } = require("electron");
const url = require("url");
const path = require("path");
let mainWindow;
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 800,
    height: 600,
    webPreferences: {
      nodeIntegration: true,
    },
    icon: path.join(__dirname, "public/greenHouse.ico"),
  });
  mainWindow.loadURL(
    url.format({
      pathname: path.join(__dirname, `/dist/casa-verde/browser/index.html`),
      protocol: "file:",
      slashes: true,
    })
  );
  mainWindow.on("closed", function () {
    mainWindow = null;
  });
  mainWindow.setMenu(null);
}
app.on("ready", createWindow);
app.on("window-all-closed", function () {
  if (process.platform !== "darwin") app.quit();
});
app.on("activate", function () {
  if (mainWindow === null) createWindow();
});
