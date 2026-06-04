package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;

public interface IPedidoFinalizacionService {

    /**
     * Ejecuta los efectos secundarios de finalizar un pedido:
     * <ol>
     *   <li>Descuenta inventario según los snapshots de composición de cada detalle.</li>
     *   <li>Acredita los fondos por moneda usada con un único movimiento COBRO_PEDIDO por moneda.</li>
     * </ol>
     * <p>
     * <strong>Precondición:</strong> el pedido debe estar en estado FINALIZADO y persisted.
     * Debe llamarse dentro de una transacción activa ({@code Propagation.MANDATORY}).
     * </p>
     *
     * @param pedido   pedido ya guardado con estado FINALIZADO
     * @param totalCop total a acreditar en COP (sin incluir el cambio al cliente)
     */
    void ejecutar(Pedido pedido, BigDecimal totalCop);
}
