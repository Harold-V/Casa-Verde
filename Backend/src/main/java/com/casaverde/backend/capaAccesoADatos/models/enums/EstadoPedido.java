package com.casaverde.backend.capaAccesoADatos.models.enums;

import java.math.BigDecimal;
import java.util.List;

public enum EstadoPedido {
    PENDIENTE,
    ABONADO_PARCIAL,
    PAGADO,
    FINALIZADO,
    ANULADO;

    public static EstadoPedido from(String value) {
        if (value == null || value.isBlank() || "ABIERTO".equalsIgnoreCase(value)) {
            return PENDIENTE;
        }
        return EstadoPedido.valueOf(value.trim().toUpperCase());
    }

    public boolean permiteEdicion() {
        return this != FINALIZADO && this != ANULADO;
    }

    public boolean esActivo() {
        return this == PENDIENTE || this == ABONADO_PARCIAL || this == PAGADO;
    }

    public static List<EstadoPedido> activos() {
        return List.of(PENDIENTE, ABONADO_PARCIAL, PAGADO);
    }

    public static EstadoPedido calcular(BigDecimal total, BigDecimal abonado) {
        if (total.compareTo(BigDecimal.ZERO) <= 0 || abonado.compareTo(BigDecimal.ZERO) <= 0)
            return PENDIENTE;
        if (abonado.compareTo(total) < 0)
            return ABONADO_PARCIAL;
        return PAGADO;
    }
}
