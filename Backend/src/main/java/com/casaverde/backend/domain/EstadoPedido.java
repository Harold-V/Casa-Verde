package com.casaverde.backend.domain;

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

    public static List<String> activos() {
        return List.of(PENDIENTE.name(), ABONADO_PARCIAL.name(), PAGADO.name(), "ABIERTO");
    }
}
