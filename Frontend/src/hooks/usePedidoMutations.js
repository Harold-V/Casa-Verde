import { useMutation, useQueryClient } from "@tanstack/react-query";
import { pedidoService } from "../services/pedidoService";

/**
 * Centraliza todas las mutations de pedidos.
 *
 * Cada mutation invalida la query ["pedidos"] automáticamente en onSuccess.
 * La lógica adicional (cerrar modales, actualizar estado local) se pasa al
 * llamar mutate(payload, { onSuccess: (res) => ... }) desde el sitio de uso.
 * TanStack Query v5 llama AMBOS onSuccess: el del hook y el del mutate().
 */
export function usePedidoMutations() {
  const qc = useQueryClient();
  const invalidar = () => qc.invalidateQueries({ queryKey: ["pedidos"] });

  /** Crear pedido nuevo */
  const crearMut = useMutation({
    mutationFn: pedidoService.crear,
    onSuccess: invalidar,
  });

  /** Agregar ítem(s) al pedido (bulk diff) */
  const agregarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.agregarDetalle(id, req),
    onSuccess: invalidar,
  });

  /** Actualizar cantidad/precio de un ítem existente */
  const actualizarDetalleMut = useMutation({
    mutationFn: ({ id, detalleId, req }) => pedidoService.actualizarDetalle(id, detalleId, req),
    onSuccess: invalidar,
  });

  /** Quitar un ítem del pedido */
  const quitarDetalleMut = useMutation({
    mutationFn: ({ id, detalleId }) => pedidoService.quitarDetalle(id, detalleId),
    onSuccess: invalidar,
  });

  /** Actualizar campos del pedido (nombre, mesa, salón, observación…) */
  const actualizarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.actualizar(id, req),
    onSuccess: invalidar,
  });

  /** Registrar un cobro/pago sobre el pedido */
  const pagoMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.registrarPago(id, req),
    onSuccess: invalidar,
  });

  /** Editar un pago ya registrado */
  const actualizarPagoMut = useMutation({
    mutationFn: ({ id, pagoId, req }) => pedidoService.actualizarPago(id, pagoId, req),
    onSuccess: invalidar,
  });

  /** Eliminar un pago del pedido */
  const quitarPagoMut = useMutation({
    mutationFn: ({ id, pagoId }) => pedidoService.quitarPago(id, pagoId),
    onSuccess: invalidar,
  });

  /** Finalizar pedido (descuenta inventario, cierra operativamente) */
  const finalizarMut = useMutation({
    mutationFn: (id) => pedidoService.finalizar(id),
    onSuccess: invalidar,
  });

  /** Anular pedido */
  const anularMut = useMutation({
    mutationFn: (id) => pedidoService.anular(id),
    onSuccess: invalidar,
  });

  return {
    invalidar,
    crearMut,
    agregarMut,
    actualizarDetalleMut,
    quitarDetalleMut,
    actualizarMut,
    pagoMut,
    actualizarPagoMut,
    quitarPagoMut,
    finalizarMut,
    anularMut,
  };
}
