import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AppLayout from "./components/layout/AppLayout";
import Dashboard from "./pages/dashboard/Dashboard";
import ConfiguracionPage from "./pages/configuracion/ConfiguracionPage";
import InventarioPage from "./pages/inventario/InventarioPage";
import Placeholder from "./pages/Placeholder";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true,           element: <Dashboard /> },
      { path: "pedidos",       element: <Placeholder titulo="Pedidos" /> },
      { path: "resumen",       element: <Placeholder titulo="Resumen del día" /> },
      { path: "inventario",    element: <InventarioPage /> },
      { path: "gastos",        element: <Placeholder titulo="Gastos" /> },
      { path: "fondos",        element: <Placeholder titulo="Fondos" /> },
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
