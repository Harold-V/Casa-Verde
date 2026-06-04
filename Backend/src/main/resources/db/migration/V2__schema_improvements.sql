-- V2: Mejoras de esquema — índices y restricciones que Hibernate ddl-auto=update
-- no puede aplicar a tablas existentes en SQLite.
-- Todos los CREATE INDEX / CREATE UNIQUE INDEX usan IF NOT EXISTS para ser idempotentes.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. RESTRICCIONES UNIQUE en catálogos
--    ddl-auto=update no puede añadir UNIQUE a una columna existente en SQLite.
--    Un UNIQUE INDEX es equivalente a una UNIQUE CONSTRAINT en SQLite.
-- ─────────────────────────────────────────────────────────────────────────────
create unique index if not exists uk_salon_nombre on
   salon (
      nombre
   );
create unique index if not exists uk_proveedor_nombre on
   proveedor (
      nombre
   );

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. ÍNDICES FK que faltaban (full table scan en cada carga de pedido)
-- ─────────────────────────────────────────────────────────────────────────────
create index if not exists idx_pd_pedido_id on
   pedido_detalle (
      pedido_id
   );
create index if not exists idx_pd_producto_id on
   pedido_detalle (
      producto_id
   );
create index if not exists idx_pago_pedido_id on
   pago (
      pedido_id
   );

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. ÍNDICES COMPUESTOS en pedido
--    Reemplaza los dos simples anteriores (idx_pedido_estado, idx_pedido_creado_en)
--    por uno compuesto que cubre la query más frecuente.
-- ─────────────────────────────────────────────────────────────────────────────
create index if not exists idx_pedido_estado_creado on
   pedido (
      estado,
      creado_en
   );
create index if not exists idx_pedido_salon_id on
   pedido (
      salon_id
   );

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. ÍNDICES en catálogos con soft-delete
-- ─────────────────────────────────────────────────────────────────────────────
create index if not exists idx_producto_activo_eliminado on
   producto (
      activo,
      eliminado
   );
create index if not exists idx_producto_categoria_id on
   producto (
      categoria_id
   );
create index if not exists idx_salon_activo_eliminado on
   salon (
      activo,
      eliminado
   );
create index if not exists idx_proveedor_activo on
   proveedor (
      activo
   );
create index if not exists idx_empleado_activo on
   empleado (
      activo
   );

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. ÍNDICES en cuenta_cobrar y gasto
-- ─────────────────────────────────────────────────────────────────────────────
create index if not exists idx_cc_cliente_nombre on
   cuenta_cobrar (
      cliente_nombre
   );
create index if not exists idx_gasto_moneda_registrado on
   gasto (
      moneda,
      registrado_en
   );
create index if not exists idx_gasto_categoria_registrado on
   gasto (
      categoria,
      registrado_en
   );

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. Columna version en adelanto (bloqueo optimista)
--    @Version requiere la columna en la tabla.
-- ─────────────────────────────────────────────────────────────────────────────
-- Use Oracle-compatible ALTER TABLE ADD syntax for the version column
alter table adelanto add (
   version number default 0
);