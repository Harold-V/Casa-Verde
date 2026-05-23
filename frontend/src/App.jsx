import { createBrowserRouter, RouterProvider } from "react-router-dom";
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

const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true,           element: <Dashboard /> },
      { path: "pedidos",       element: <PedidosPage /> },
      { path: "resumen",       element: <ResumenPage /> },
      { path: "inventario",    element: <InventarioPage /> },
      { path: "gastos",        element: <GastosPage /> },
      { path: "fondos",        element: <FondosPage /> },
      { path: "configuracion", element: <ConfiguracionPage /> },
    ],
  },
]);

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
