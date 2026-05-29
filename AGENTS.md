# AGENTS.md

## Propósito

Este repositorio implementa un sistema de escritorio para la operación de un asadero/restaurante con foco en rapidez operativa, trazabilidad financiera, control de inventario y ejecución local.

Codex debe comportarse como:

- arquitecto de software senior,
- analista funcional,
- desarrollador full-stack senior,
- revisor de calidad técnica.

La prioridad no es una demo visual; la prioridad es un sistema operable de verdad, consistente, mantenible y preparado para crecer de monousuario local a esquema cliente-servidor.

---

## Objetivo del sistema

Construir una aplicación administrativa tipo panel de operación para gestionar:

- pedidos,
- abonos y cobros,
- pedidos pendientes,
- resumen del día,
- inventario,
- gastos,
- capital neto acumulado,
- fondos en COP, USD y Nequi,
- configuración general,
- catálogo de productos,
- catálogo de salones,
- tasa del dólar.

El sistema corre localmente, usando SQLite, Electron, React y Spring Boot.

---

## Stack obligatorio

No proponer variantes salvo que se justifique explícitamente para una mejora futura.

- Base de datos: SQLite.
- Frontend desktop: React + Tailwind+ Electron.
- Backend: Java + Spring Boot.
- Arquitectura preparada para evolución futura a multiusuario o cliente-servidor.

### Decisiones base

- Electron embebe el frontend React y arranca el backend local.
- Spring Boot expone API REST local en `localhost`.
- SQLite es la persistencia principal del entorno local.
- La lógica de negocio vive en backend, no en Electron ni solo en el frontend.
- El frontend debe ser delgado, orientado a operación y validación visual.

---

## Principios de diseño

Cuando generes código, diseño o decisiones técnicas, sigue estas reglas:

1. Priorizar velocidad operativa.
2. Minimizar cantidad de clics.
3. Garantizar trazabilidad de dinero, inventario y cambios críticos.
4. Mantener separación clara entre UI, lógica de negocio y persistencia.
5. Diseñar para migración futura a PostgreSQL y backend remoto.
6. No acoplar decisiones de negocio a SQLite de forma irreversible.
7. Toda operación crítica debe ser transaccional.
8. Toda regla crítica debe validarse en backend aunque ya exista validación en frontend.
9. No usar lógica oculta en frontend para cálculos financieros finales.
10. Cada módulo debe tener vista de listado y vista de detalle.

---

## UX/UI obligatoria

La aplicación es un admin panel de escritorio, no una landing page.

### Layout general

- Sidebar como navegación principal.
- Topbar para acciones secundarias y contexto.
- En escritorio, el sidebar debe permanecer visible.
- En pantallas pequeñas, el sidebar puede colapsarse.
- La estructura visual estándar debe ser:
  - KPIs arriba,
  - filtros en zona media,
  - detalle o listado abajo.

### Criterios de usabilidad

- Formularios rápidos.
- Acciones frecuentes visibles sin navegar demasiado.
- Validación clara cerca del campo.
- Estados con color y texto.
- Búsqueda rápida de productos.
- Modales solo cuando aceleren la operación.
- Confirmaciones únicamente en acciones destructivas o irreversibles.

### Pantallas mínimas

- Dashboard.
- Pedidos.
- Pedidos pendientes.
- Resumen del día.
- Inventario.
- Gastos.
- Capital y fondos.
- Configuración.
- Catálogo de productos.
- Catálogo de salones.

---

## Arquitectura esperada

### Backend

Organizar en capas exactas o equivalentes:

- `controller`
- `service`
- `repository`
- `domain/model`
- `dto`
- `mapper`
- `exceptions`

### Reglas backend

- Los controladores no contienen lógica de negocio compleja.
- Los services contienen orquestación, validaciones de negocio y transacciones.
- Los repositories solo resuelven acceso a datos.
- Los DTO separan contrato externo del modelo persistido.
- Los mappers convierten entre entidad y DTO.
- Las excepciones de negocio deben ser explícitas y centralizadas.
- Debe existir manejo global de errores.

### Frontend

Sugerencia organizativa:

- `modules/` por dominio funcional.
- `shared/` para componentes reutilizables.
- `layouts/` para sidebar, topbar y contenedores.
- `hooks/` para lógica UI.
- `services/` o `api/` para consumo REST.
- `types/` para contratos TypeScript.

### Escalabilidad futura

Preparar el código para que luego sea posible:

- reemplazar SQLite por PostgreSQL,
- mover Spring Boot fuera del equipo local,
- habilitar autenticación por usuario,
- soportar múltiples estaciones operando sobre la misma base.

---

## Modelo mental del dominio

Codex debe razonar con estos núcleos funcionales:

### 1. Pedido

Un pedido pertenece a un cliente, tiene ítems, abonos, saldo pendiente, estado y eventualmente cierre.

### 2. Pago

Un pedido puede tener múltiples pagos y pagos mixtos. Cada movimiento debe registrar moneda, método, tasa aplicada, equivalente en moneda base y trazabilidad temporal.

### 3. Inventario

Inicialmente controla gallinas y cuyes, pero debe ser extensible a más insumos o productos inventariables.

### 4. Caja/Fondos

No mezclar COP, USD y Nequi como si fueran el mismo fondo. Deben convivir en consolidado, pero mantenerse separados operativamente.

### 5. Gasto

Todo gasto impacta fondos. Algunos gastos además impactan inventario.

### 6. Configuración

La configuración no es decorativa; debe dejar el sistema listo para operar desde el primer día.

---

## Reglas funcionales que no se deben violar

### Pedidos

- `nombreCliente` es obligatorio.
- Si no se informa fecha u hora, usar fecha y hora actual del sistema.
- El pedido puede tener múltiples productos.
- Se puede agregar producto desde catálogo.
- Se puede agregar producto manual no catalogado.
- Cada ítem del pedido debe guardar:
  - nombre,
  - cantidad,
  - precio unitario,
  - subtotal,
  - observación opcional.
- El backend debe calcular automáticamente:
  - total,
  - abonos acumulados,
  - saldo pendiente,
  - estado.

### Estados del pedido

Estados válidos sugeridos:

- `PENDIENTE`
- `ABONADO_PARCIAL`
- `PAGADO`
- `FINALIZADO`
- `ANULADO`

Regla:

- un pedido solo puede pasar a `FINALIZADO` cuando está completamente pagado.

### Pagos, abonos y cobros

- Soportar COP, USD y Nequi.
- Soportar pago mixto entre esos tres fondos.
- Calcular automáticamente:
  - total recibido,
  - total abonado acumulado,
  - saldo pendiente,
  - valor de cambio.
- Si hay USD, usar la tasa del dólar configurada.
- Registrar trazabilidad de cada movimiento con:
  - fecha,
  - hora,
  - tipo de movimiento,
  - método,
  - moneda,
  - monto original,
  - tasa aplicada si corresponde,
  - equivalente en moneda base.

### Pedidos pendientes

La pantalla de pendientes debe mostrar pedidos no finalizados o con saldo pendiente.
Debe permitir:

- ver factura,
- editar pedido,
- editar ítems,
- ver pagos previos,
- registrar nuevos cobros,
- finalizar cuando saldo sea cero.

### Resumen del día

Debe permitir filtro por fecha y mostrar:

- pedidos finalizados del día,
- cliente,
- total,
- hora de cierre.

KPIs mínimos:

- total vendido,
- número de pedidos finalizados,
- cantidad de gallinas vendidas,
- cantidad de cuyes vendidos,
- ingresos por COP,
- ingresos por USD,
- ingresos por Nequi,
- gastos del día,
- balance del día.

### Inventario

Inicialmente controlar:

- gallinas,
- cuyes.

Debe soportar:

- entradas,
- salidas,
- ajustes manuales,
- consulta de stock,
- historial auditado.

Regla crítica:

- al finalizar un pedido, descontar inventario automáticamente según la composición de sus productos.

### Gastos

Registrar:

- compra de cuyes,
- compra de gallinas,
- gastos operativos,
- gastos varios.

Cada gasto debe guardar:

- fecha,
- hora,
- tipo,
- descripción,
- valor,
- fondo usado,
- observaciones.

Si el gasto compra gallinas o cuyes:

- puede impactar inventario,
- debe afectar fondos,
- debe afectar capital neto acumulado.

### Capital y fondos

Gestionar por separado:

- disponible en COP,
- disponible en USD,
- disponible en Nequi,
- capital neto acumulado total.

El consolidado total puede calcularse usando la tasa del dólar configurada.
Nequi siempre debe verse separado.

### Configuración

Debe permitir administrar:

- tasa del dólar,
- parámetros generales,
- productos,
- precios,
- salones,
- capital inicial si aplica.

La tasa del dólar debe guardar fecha de actualización y preferiblemente historial.

---

## Entidades mínimas esperadas

Cuando modeles dominio, incluye como mínimo estas entidades o equivalentes:

- Usuario
- Rol
- Pedido
- DetallePedido
- Producto
- CategoriaProducto
- ProductoComposicionInventario
- Pago
- MetodoPago
- Moneda
- TasaCambio
- Inventario
- MovimientoInventario
- Gasto
- TipoGasto
- CajaMovimiento
- Salon
- ConfiguracionGeneral
- HistorialAuditoria

No eliminar estas entidades por simplificación apresurada. Si se consolidan, justificarlo claramente.

---

## Catálogo inicial obligatorio

El sistema debe venir preparado con este catálogo base:

- Cuy
- 1/2 Cuy
- 1/4 Cuy
- Gallina
- 1/2 Gallina
- 1/8 Gallina
- Almuerzo 1/8 gallina y 1/2 cuy
- Almuerzo de 1/8 gallina y 1/4 cuy
- Consomé
- Consomé desmechado
- Porción Arroz
- Porción Papa
- Gaseosa
- Jugo Hit
- Agua
- Cerveza
- Pokeron

Cada producto debe permitir:

- precio configurable,
- edición,
- activación/inactivación,
- composición de inventario configurable.

---

## Composición de inventario por producto

El sistema debe permitir definir el impacto inventariable por producto.

Ejemplos obligatorios:

- Cuy -> 1 cuy
- 1/2 Cuy -> 0.5 cuy
- 1/4 Cuy -> 0.25 cuy
- Gallina -> 1 gallina
- 1/2 Gallina -> 0.5 gallina
- 1/8 Gallina -> 0.125 gallina
- Almuerzo 1/8 gallina y 1/2 cuy -> 0.125 gallina + 0.5 cuy
- Almuerzo de 1/8 gallina y 1/4 cuy -> 0.125 gallina + 0.25 cuy
- Bebidas o complementos inicialmente no afectan inventario animal

Nunca hardcodear estas reglas solo en frontend. Deben quedar persistidas y administrables.

---

## Requisitos no funcionales

- Arquitectura limpia y desacoplada.
- Persistencia local con SQLite.
- Buen manejo de validaciones y errores.
- Auditoría básica de cambios críticos.
- Diseño responsive dentro de Electron.
- Código mantenible y escalable.
- Preparado para exportación futura a PDF o Excel.

### Calidad técnica mínima

- Sin lógica duplicada entre frontend y backend.
- Sin cálculos monetarios ambiguos.
- Sin uso indiscriminado de tipos `double` para dinero en backend.
- Sin queries N+1 evitables.
- Sin strings mágicos repartidos por todo el código.

---

## Reglas de implementación para Codex

### Al generar backend

- Usar Java moderno y buenas prácticas de Spring Boot.
- Separar request DTO y response DTO.
- Usar servicios transaccionales para operaciones críticas.
- Crear validaciones de negocio explícitas.
- Centralizar enums para estados, monedas y tipos de movimiento.
- Guardar timestamps de creación y actualización.
- Hacer persistencia compatible con SQLite.
- Diseñar repositorios y servicios pensando en futura migración a PostgreSQL.

### Al generar frontend

- Crear vistas de listado y detalle por módulo.
- Mantener un diseño consistente de admin panel.
- Priorizar accesos rápidos a crear pedido, cobrar y registrar gasto.
- Hacer formularios con validación clara.
- Diseñar tablas prácticas, no decorativas.
- Mostrar estados, saldos y alertas de forma visible.
- Evitar componentes innecesariamente complejos.

### Al generar SQL

- Incluir llaves primarias.
- Incluir llaves foráneas cuando apliquen.
- Incluir índices relevantes.
- Incluir restricciones `CHECK` cuando apliquen.
- Pensar la integridad para SQLite, no para otro motor.
- Mantener nombres consistentes y fáciles de mapear desde JPA o JDBC.

### Al generar endpoints

- Ser concreto.
- Nombrar recursos de forma RESTful.
- No mezclar operaciones masivas y acciones de dominio en rutas ambiguas.
- Para acciones de dominio válidas, se permiten rutas tipo:
  - `/pedidos/{id}/pagos`
  - `/pedidos/{id}/finalizar`
  - `/pedidos/{id}/anular`

---

## Convenciones de respuesta de Codex

Cuando respondas o generes cambios en este repositorio:

1. No dar explicaciones genéricas.
2. Justificar decisiones importantes en términos de operación del asadero.
3. Si hay ambigüedad, resolver con una decisión razonable y explícita.
4. Si la decisión puede afectar dinero, inventario o auditoría, explicarla claramente.
5. Entregar resultados accionables.
6. Cuando generes código, procurar que compile o quede muy cerca de compilar.
7. No introducir librerías o frameworks no solicitados sin justificar su valor.
8. Si cambias estructura del proyecto, explicar impacto y motivo.
9. Si generas SQL, mantener consistencia con el modelo Java.
10. Si generas UI, respetar el layout sidebar + topbar + contenido operativo.

---

## Qué debe evitar Codex

- No convertir esto en POS de mesas genérico sin contexto del asadero.
- No simplificar pagos mezclando Nequi con efectivo COP.
- No eliminar historial de movimientos “por facilidad”.
- No calcular inventario solo con stock agregado sin historial.
- No modelar pedidos sin detalle de pagos.
- No hacer toda la lógica en React.
- No proponer Firebase, MongoDB, Supabase o stacks alternos para el núcleo del sistema.
- No usar arquitectura innecesariamente compleja para un entorno local MVP.
- No omitir restricciones de integridad por comodidad.
- No asumir que un producto manual impacta inventario si no tiene composición definida.

---

## Decisiones razonables ya tomadas

Para evitar bloqueos por ambigüedad, asumir lo siguiente salvo instrucción contraria:

- La moneda base del sistema es COP.
- Nequi se maneja como fondo separado, aunque contablemente se consolide en COP.
- Un pedido puede quedar `PAGADO` antes de `FINALIZADO`.
- `FINALIZADO` implica cierre operativo del pedido.
- El descuento de inventario se hace al finalizar, no al crear el pedido.
- Los productos manuales no afectan inventario por defecto.
- La tasa USD activa se toma desde configuración al registrar un pago en USD.
- Todo movimiento de caja debe quedar trazado.
- Todo cambio crítico debe quedar listo para auditoría.

---

## Estructura sugerida del proyecto

```text
root/
  backend/
    src/main/java/.../
      controller/
      service/
      repository/
      domain/
      dto/
      mapper/
      exceptions/
      config/
  frontend/
    src/
      app/
      modules/
      components/
      layouts/
      services/
      hooks/
      types/
  electron/
  database/
  docs/
```

Si el repositorio aún no tiene estructura definitiva, usar esta como base.

---

## Orden recomendado de implementación

Si no se especifica otra tarea, priorizar en este orden:

1. Modelo de datos SQLite.
2. Entidades, enums y repositorios backend.
3. Servicios de negocio de pedidos, pagos, gastos, inventario y caja.
4. Endpoints REST principales.
5. Pantallas core de operación.
6. Configuración inicial.
7. Dashboard y resumen del día.
8. Auditoría y mejoras.

---

## Flujo crítico que nunca debe romperse

### Flujo de pedido

1. Crear pedido.
2. Agregar ítems.
3. Calcular total.
4. Registrar uno o varios pagos.
5. Calcular saldo pendiente.
6. Si saldo es cero, permitir finalizar.
7. Al finalizar:
   - cerrar pedido,
   - descontar inventario,
   - consolidar trazabilidad,
   - reflejar impacto en reportes.

### Flujo de gasto

1. Registrar gasto.
2. Descontar del fondo correspondiente.
3. Si compra inventariable, aumentar stock.
4. Reflejar impacto en capital neto.
5. Registrar auditoría o historial del movimiento.

---

## Definición de terminado

Una tarea se considera realmente terminada solo si cumple esto:

- compila o queda estructuralmente consistente,
- respeta el dominio del asadero,
- no rompe reglas de negocio clave,
- no compromete trazabilidad,
- incluye validaciones mínimas,
- conserva coherencia con el stack obligatorio,
- deja base escalable para iteraciones futuras.

---

## Instrucción final para Codex

Al trabajar en este proyecto, pensar siempre en un sistema real de operación diaria: rápido para cobrar, claro para revisar pendientes, confiable para dinero e inventario, y simple de mantener.

No optimizar para “verse bonito” antes de asegurar integridad operativa.
