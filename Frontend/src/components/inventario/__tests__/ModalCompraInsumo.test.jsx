import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

vi.mock("../../../services/gastoService", () => ({
  gastoService: { registrar: vi.fn() },
}));

vi.mock("../../../services/fondoService", () => ({
  fondoService: {
    listar: vi.fn().mockResolvedValue([
      { moneda: "COP", saldo: 1000000 },
      { moneda: "USD", saldo: 200 },
    ]),
  },
}));

import ModalCompraInsumo from "../ModalCompraInsumo";
import { gastoService } from "../../../services/gastoService";

const INSUMOS = [
  { insumo: "gallina", stockActual: 10 },
  { insumo: "cuy",     stockActual: 5  },
];

function wrapper({ children }) {
  return (
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      {children}
    </QueryClientProvider>
  );
}

describe("ModalCompraInsumo", () => {
  let onClose;

  beforeEach(() => {
    onClose = vi.fn();
    vi.clearAllMocks();
  });

  it("muestra el título 'Registrar entrada de insumo'", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    expect(screen.getByText("Registrar entrada de insumo")).toBeInTheDocument();
  });

  it("renderiza los campos insumo, cantidad, moneda y monto", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    expect(screen.getByLabelText(/insumo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/cantidad/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/moneda/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/monto/i)).toBeInTheDocument();
  });

  it("NO muestra campo de observación", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    expect(screen.queryByLabelText(/observaci/i)).not.toBeInTheDocument();
  });

  it("muestra las opciones de insumo (gallina y cuy)", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    const select = screen.getByLabelText(/insumo/i);
    expect(select).toHaveValue("gallina");
    const options = Array.from(select.options).map((o) => o.value);
    expect(options).toContain("gallina");
    expect(options).toContain("cuy");
  });

  it("genera concepto 'Compra 5 gallinas' al registrar 5 gallinas", async () => {
    gastoService.registrar.mockResolvedValue({});
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.clear(screen.getByLabelText(/cantidad/i));
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.clear(screen.getByLabelText(/monto/i));
    await userEvent.type(screen.getByLabelText(/monto/i), "150000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await waitFor(() =>
      expect(gastoService.registrar).toHaveBeenCalledWith(
        expect.objectContaining({ concepto: "Compra 5 gallinas" })
      )
    );
  });

  it("genera concepto 'Compra 3 cuyes' al registrar 3 cuyes", async () => {
    gastoService.registrar.mockResolvedValue({});
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.selectOptions(screen.getByLabelText(/insumo/i), "cuy");
    await userEvent.clear(screen.getByLabelText(/cantidad/i));
    await userEvent.type(screen.getByLabelText(/cantidad/i), "3");
    await userEvent.clear(screen.getByLabelText(/monto/i));
    await userEvent.type(screen.getByLabelText(/monto/i), "90000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await waitFor(() =>
      expect(gastoService.registrar).toHaveBeenCalledWith(
        expect.objectContaining({ concepto: "Compra 3 cuyes" })
      )
    );
  });

  it("envía payload completo correcto", async () => {
    gastoService.registrar.mockResolvedValue({});
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.clear(screen.getByLabelText(/cantidad/i));
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.clear(screen.getByLabelText(/monto/i));
    await userEvent.type(screen.getByLabelText(/monto/i), "150000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await waitFor(() =>
      expect(gastoService.registrar).toHaveBeenCalledWith({
        concepto: "Compra 5 gallinas",
        categoria: "insumos",
        moneda: "COP",
        monto: 150000,
        insumoInventario: "gallina",
        cantidadInventario: 5,
      })
    );
  });

  it("muestra error si cantidad está vacía", async () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    await userEvent.type(screen.getByLabelText(/monto/i), "50000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));
    expect(screen.getByText(/cantidad debe ser al menos 1/i)).toBeInTheDocument();
  });

  it("muestra error si monto está vacío", async () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));
    expect(screen.getByText(/monto debe ser mayor a 0/i)).toBeInTheDocument();
  });

  it("muestra error del backend si el servicio falla", async () => {
    gastoService.registrar.mockRejectedValue(new Error("Saldo insuficiente en el fondo"));
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });

    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.type(screen.getByLabelText(/monto/i), "150000");
    await userEvent.click(screen.getByRole("button", { name: /registrar entrada/i }));

    await screen.findByText("Saldo insuficiente en el fondo");
  });

  it("llama onClose al hacer clic en Cancelar", async () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    await userEvent.click(screen.getByRole("button", { name: /cancelar/i }));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("llama onClose al presionar Escape", () => {
    render(<ModalCompraInsumo insumos={INSUMOS} onClose={onClose} />, { wrapper });
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledOnce();
  });
});
