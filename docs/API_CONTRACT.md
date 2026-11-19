# Contrato de integración — Taquilla de Cine API

Documento de referencia para el frontend Angular 21. Describe el contrato **real**
expuesto por el backend en la fase 2.6. Los nombres, tipos y límites provienen
directamente del código fuente y de respuestas HTTP verificadas contra el JAR
empaquetado.

- **URL base (desarrollo):** `http://localhost:8080`
- **Prefijo de negocio:** `/api`
- **Formato:** JSON UTF-8 (`Content-Type: application/json`)
- **Autenticación:** no implementada (fuera del alcance del proyecto)
- **Zona horaria de negocio:** `America/Bogota` (UTC−5) para los reportes

---

## 1. Resumen de endpoints

| # | Método | Ruta | Éxito | Descripción |
|---:|---|---|---|---|
| 1 | `GET` | `/api/peliculas` | `200` | Lista todas las películas |
| 2 | `GET` | `/api/peliculas/{id}` | `200` | Consulta una película |
| 3 | `POST` | `/api/peliculas` | `201` | Registra una película |
| 4 | `PUT` | `/api/peliculas/{id}` | `200` | Actualiza una película |
| 5 | `DELETE` | `/api/peliculas/{id}` | `204` | Elimina una película sin funciones |
| 6 | `GET` | `/api/funciones` | `200` | Lista todas las funciones |
| 7 | `GET` | `/api/funciones/{id}` | `200` | Consulta una función |
| 8 | `POST` | `/api/funciones` | `201` | Programa una función |
| 9 | `PUT` | `/api/funciones/{id}` | `200` | Actualiza una función |
| 10 | `DELETE` | `/api/funciones/{id}` | `204` | Elimina una función sin compras |
| 11 | `GET` | `/api/compras` | `200` | Lista todas las compras |
| 12 | `POST` | `/api/compras` | `201` | Registra una compra |
| 13 | `GET` | `/api/reportes/ventas` | `200` | Reporte de ventas por rango de días |
| — | `GET` | `/actuator/health` | `200` | Estado del backend |

No existen endpoints `PUT`, `PATCH` ni `DELETE` para compras, ni consulta
individual `GET /api/compras/{id}`. Tampoco hay paginación, filtros ni
ordenamiento configurable.

---

## 2. Convenciones de tipos y formatos

| Dato | Formato JSON | Ejemplo | Notas |
|---|---|---|---|
| Identificadores | número entero | `770` | PostgreSQL `BIGINT` → `Long` |
| Dinero | número decimal | `15000.00`, `75000.00` | PostgreSQL `NUMERIC(12,2)` → `BigDecimal`; siempre 2 decimales |
| Cantidades | número entero | `3`, `95` | `Integer` / `long` |
| Fecha | cadena `YYYY-MM-DD` | `2026-12-01` | `LocalDate` |
| Hora | cadena `HH:mm:ss` | `18:30:00` | `LocalTime`; puede llegar `18:30` y el servidor la normaliza |
| Fecha y hora de compra | cadena ISO-8601 con offset | `2026-11-17T20:00:05.552265-05:00` | `OffsetDateTime`; representa un instante |
| `timestamp` de error | cadena ISO local sin offset | `2026-11-17T20:00:32.5743828` | `LocalDateTime` del servidor |
| Cadenas de texto | cadena | `"Interestelar"` | UTF-8 |

> **Importante para Angular:** `total` y `totalRecaudado` llegan como números
> JSON. Para mostrar moneda use `Intl.NumberFormat('es-CO', { style: 'currency',
> currency: 'COP' })`. Evite operaciones aritméticas monetarias en JavaScript;
> los cálculos oficiales los realiza el backend con `BigDecimal`.

### Límites de longitud y validación

| Campo | Límite / regla |
|---|---|
| `titulo` | obligatorio, máximo 150 caracteres |
| `genero` | obligatorio, máximo 80 caracteres |
| `clasificacion` | obligatorio, máximo 20 caracteres |
| `duracionMin` | obligatorio, entero ≥ 1 |
| `sala` | obligatorio, máximo 50 caracteres |
| `precio` | obligatorio, > 0, máximo 10 dígitos enteros y 2 decimales (`NUMERIC(12,2)`) |
| `cuposTotales` | obligatorio, entero ≥ 1 |
| `nombreCliente` | obligatorio, máximo 150 caracteres |
| `cantidadBoletos` | obligatorio, entero ≥ 1 |
| `peliculaId`, `funcionId` | obligatorios, enteros positivos |

`cuposDisponibles` **no forma parte de ningún request**. Si se envía en el JSON,
el servidor lo ignora: en la creación siempre se inicializa igual a
`cuposTotales` y en la actualización se recalcula a partir de los boletos ya
vendidos.

---

## 3. Películas

### 3.1 `GET /api/peliculas`

- **Parámetros:** ninguno.
- **Respuesta `200`:** arreglo ordenado por `id` ascendente.
- **Errores:** `500` ante fallo inesperado.

Ejemplo real:

```json
[
  {"id": 770, "titulo": "Interestelar", "genero": "Ciencia ficcion", "duracionMin": 169, "clasificacion": "PG-13"}
]
```

### 3.2 `GET /api/peliculas/{id}`

- **Parámetros de ruta:** `id` entero.
- **Respuesta `200`:** objeto `PeliculaResponse`.
- **Errores:**
  - `400` si `id` no es numérico (`Parámetro inválido`).
  - `404` si no existe (`Recurso no encontrado`).

### 3.3 `POST /api/peliculas`

- **Cuerpo:** `PeliculaRequest`.
- **Respuesta `201`:**
  - Encabezado `Location: /api/peliculas/{id}` (URI relativa).
  - Cuerpo `PeliculaResponse`.
- **Errores:** `400` por validación o JSON malformado; `415` si el
  `Content-Type` no es JSON.

Ejemplo real de respuesta `201`:

```json
{"id": 770, "titulo": "Interestelar", "genero": "Ciencia ficcion", "duracionMin": 169, "clasificacion": "PG-13"}
```

### 3.4 `PUT /api/peliculas/{id}`

- **Cuerpo:** `PeliculaRequest` completo (reemplazo de los cuatro campos).
- **Respuesta `200`:** `PeliculaResponse` actualizado.
- **Errores:** `400`, `404` si la película no existe.

### 3.5 `DELETE /api/peliculas/{id}`

- **Respuesta `204`:** sin cuerpo.
- **Errores:**
  - `404` si la película no existe.
  - `409` (`Conflicto de integridad`) si tiene funciones asociadas. El mensaje es
    `"No se puede eliminar la película porque tiene funciones asociadas"`.

> La base de datos usa `ON DELETE RESTRICT`; la API no elimina en cascada.

---

## 4. Funciones

### 4.1 `GET /api/funciones`

- **Respuesta `200`:** arreglo ordenado por `fecha` ASC, `hora` ASC, `id` ASC.
- Incluye `tituloPelicula` obtenido mediante `JOIN` con `peliculas`.
- **Errores:** `500`.

### 4.2 `GET /api/funciones/{id}`

- **Respuesta `200`:** `FuncionResponse`.
- **Errores:** `400` si `id` no es numérico; `404` si no existe.

Ejemplo real:

```json
{"id": 545, "peliculaId": 770, "tituloPelicula": "Interestelar", "fecha": "2026-12-01", "hora": "18:30:00", "sala": "Sala 1", "precio": 15000.00, "cuposTotales": 100, "cuposDisponibles": 100}
```

### 4.3 `POST /api/funciones`

- **Cuerpo:** `FuncionRequest`.
- **Respuesta `201`:**
  - Encabezado `Location: /api/funciones/{id}`.
  - Cuerpo `FuncionResponse` con `cuposDisponibles = cuposTotales`.
- **Errores:**
  - `400` por validación o JSON malformado.
  - `404` si `peliculaId` no existe.

### 4.4 `PUT /api/funciones/{id}`

- **Cuerpo:** `FuncionRequest` completo.
- **Respuesta `200`:** `FuncionResponse` actualizado.
- **Errores:**
  - `400` por validación.
  - `404` si la función o la nueva película no existen.
  - `409` si la nueva capacidad es menor que los boletos ya vendidos.
  - `409` si se intenta cambiar la película de una función con compras.

Regla de capacidad: `cuposDisponibles = cuposTotales - boletosVendidos`.

### 4.5 `DELETE /api/funciones/{id}`

- **Respuesta `204`:** sin cuerpo.
- **Errores:**
  - `404` si no existe.
  - `409` si tiene compras asociadas:
    `"No se puede eliminar la función porque tiene compras asociadas"`.

---

## 5. Compras

### 5.1 `GET /api/compras`

- **Respuesta `200`:** arreglo ordenado por `fechaCompra` DESC y `id` DESC.
- No incluye el título de la película ni datos de la función. Angular debe
  cruzar `funcionId` con `GET /api/funciones` si necesita mostrarlos.

Ejemplo real:

```json
[
  {"id": 668, "funcionId": 545, "nombreCliente": "Cliente E2E B", "cantidadBoletos": 2, "total": 30000.00, "fechaCompra": "2026-11-17T20:00:05.612556-05:00"},
  {"id": 667, "funcionId": 545, "nombreCliente": "Cliente E2E A", "cantidadBoletos": 3, "total": 45000.00, "fechaCompra": "2026-11-17T20:00:05.552265-05:00"}
]
```

### 5.2 `POST /api/compras`

- **Cuerpo:** `CompraRequest`.
- **Respuesta `201`:** `CompraResponse`.
  - **No** incluye encabezado `Location`, porque no existe un recurso individual
    de compra.
- **Errores:**
  - `400` por validación, JSON malformado o total fuera de `NUMERIC(12,2)`.
  - `404` si la función no existe.
  - `409` (`Cupos insuficientes`) si `cantidadBoletos` supera los disponibles.

Ejemplo real de compra exitosa:

```json
{"id": 667, "funcionId": 545, "nombreCliente": "Cliente E2E A", "cantidadBoletos": 3, "total": 45000.00, "fechaCompra": "2026-11-17T20:00:05.552265-05:00"}
```

Ejemplo real de sobreventa rechazada:

```json
{"status": 409, "error": "Cupos insuficientes", "mensaje": "Se solicitaron 96 boletos, pero solo hay 95 disponibles", "ruta": "/api/compras", "timestamp": "2026-11-17T20:00:05.7281252"}
```

Garantías:

- El registro de la compra y el descuento de cupos ocurren en una sola
  transacción.
- Se bloquea la fila de la función con `SELECT ... FOR UPDATE`, por lo que las
  compras concurrentes se serializan y no producen sobreventa.
- Una compra rechazada no crea registros ni modifica `cuposDisponibles`.

---

## 6. Reporte de ventas

### 6.1 `GET /api/reportes/ventas`

- **Parámetros de consulta obligatorios:**
  - `fechaDesde` (`YYYY-MM-DD`)
  - `fechaHasta` (`YYYY-MM-DD`)
- **Semántica:** rango inclusivo de días en `America/Bogota`. Internamente se
  consulta el intervalo semiabierto `[fechaDesde 00:00, fechaHasta + 1 día 00:00)`.
- **Respuesta `200`:** siempre, incluso si no hay ventas (`compras: []`,
  `cantidadCompras: 0`, `totalBoletos: 0`, `totalRecaudado: 0.00`).
- **Errores:**
  - `400` si falta un parámetro (`Parámetro faltante`).
  - `400` si el formato no es `YYYY-MM-DD` o la fecha no existe
    (`Parámetro inválido`).
  - `400` si `fechaDesde > fechaHasta` (`Rango de fechas inválido`).

Ejemplo real (rango de un día, con dos compras):

```json
{
  "fechaDesde": "2026-11-17",
  "fechaHasta": "2026-11-17",
  "cantidadCompras": 2,
  "totalBoletos": 5,
  "totalRecaudado": 75000.00,
  "compras": [
    {
      "id": 668,
      "funcionId": 545,
      "tituloPelicula": "Interestelar",
      "fechaFuncion": "2026-12-01",
      "horaFuncion": "18:30:00",
      "sala": "Sala 1",
      "nombreCliente": "Cliente E2E B",
      "cantidadBoletos": 2,
      "total": 30000.00,
      "fechaCompra": "2026-11-17T20:00:05.612556-05:00"
    },
    {
      "id": 667,
      "funcionId": 545,
      "tituloPelicula": "Interestelar",
      "fechaFuncion": "2026-12-01",
      "horaFuncion": "18:30:00",
      "sala": "Sala 1",
      "nombreCliente": "Cliente E2E A",
      "cantidadBoletos": 3,
      "total": 45000.00,
      "fechaCompra": "2026-11-17T20:00:05.552265-05:00"
    }
  ]
}
```

Observaciones:

- `fechaCompra` se normaliza explícitamente a `-05:00`.
- Los importes salen de `compras.total` (importe histórico), no del precio
  actual de la función.
- `tituloPelicula`, `fechaFuncion`, `horaFuncion` y `sala` son los datos
  **actuales** de la película y la función relacionadas; no son una copia
  histórica inmutable.
- `cantidadCompras`, `totalBoletos` y `totalRecaudado` se calculan sobre la misma
  lista `compras`, por lo que siempre son consistentes entre sí.

---

## 7. Salud del servicio

### `GET /actuator/health`

- **Respuesta `200`:** `Content-Type: application/vnd.spring-boot.actuator.v3+json`
- **Cuerpo real:**

```json
{"groups": ["liveness", "readiness"], "status": "UP"}
```

No se exponen componentes, detalles, variables de entorno ni información de la
base de datos (`management.endpoint.health.show-details=never`). Este endpoint
no está bajo `/api/**` y no tiene configuración CORS.

---

## 8. Formato global de errores

Todas las respuestas de error del backend (excepto el rechazo CORS del
contenedor) usan este objeto:

```json
{
  "status": 400,
  "error": "Datos de entrada inválidos",
  "mensaje": "Validación fallida en uno o más campos",
  "ruta": "/api/peliculas",
  "timestamp": "2026-11-17T20:00:32.5743828"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `status` | number | Código HTTP repetido en el cuerpo |
| `error` | string | Título corto y estable del tipo de error |
| `mensaje` | string | Explicación legible, sin SQL ni trazas |
| `ruta` | string | Ruta solicitada |
| `timestamp` | string | Fecha y hora local del servidor |

### Errores de validación

Añaden el campo `errores`, un mapa `campo → mensaje`:

```json
{
  "status": 400,
  "error": "Datos de entrada inválidos",
  "mensaje": "Validación fallida en uno o más campos",
  "ruta": "/api/peliculas",
  "timestamp": "2026-11-17T20:00:32.5743828",
  "errores": {
    "titulo": "El título es obligatorio",
    "genero": "El género es obligatorio",
    "duracionMin": "La duración debe ser mayor que cero",
    "clasificacion": "La clasificación es obligatoria"
  }
}
```

El orden de las claves de `errores` no es estable.

### Catálogo de errores verificados

| HTTP | `error` | Cuándo |
|---:|---|---|
| `400` | `Datos de entrada inválidos` | Falla Bean Validation; incluye `errores` |
| `400` | `JSON inválido` | Cuerpo malformado o tipo incompatible |
| `400` | `Parámetro inválido` | `{id}` no numérico o fecha con formato inválido |
| `400` | `Parámetro faltante` | Falta `fechaDesde` o `fechaHasta` |
| `400` | `Rango de fechas inválido` | `fechaDesde > fechaHasta` |
| `400` | `Monto fuera de rango` | Total de compra excede `NUMERIC(12,2)` |
| `404` | `Recurso no encontrado` | ID inexistente o ruta desconocida |
| `405` | `Método no permitido` | Método no soportado por la ruta |
| `409` | `Cupos insuficientes` | Compra mayor a los cupos disponibles |
| `409` | `Conflicto de integridad` | Borrado restringido por FK o capacidad inválida |
| `415` | `Tipo de contenido no soportado` | `Content-Type` distinto de JSON |
| `500` | `Error interno del servidor` | Fallo inesperado; mensaje genérico |

Ejemplos reales:

```json
{"status": 404, "error": "Recurso no encontrado", "mensaje": "Película no encontrada con id: 999999999", "ruta": "/api/peliculas/999999999", "timestamp": "2026-11-17T20:00:32.8186963"}
```

```json
{"status": 400, "error": "Parámetro inválido", "mensaje": "El parámetro 'id' tiene un valor o formato inválido", "ruta": "/api/funciones/abc", "timestamp": "2026-11-17T20:00:32.6568142"}
```

```json
{"status": 405, "error": "Método no permitido", "mensaje": "El método POST no está permitido para este recurso", "ruta": "/api/peliculas/1", "timestamp": "2026-11-17T20:00:32.7074644"}
```

```json
{"status": 415, "error": "Tipo de contenido no soportado", "mensaje": "El tipo de contenido 'text/plain;charset=UTF-8' no está soportado; utilice application/json", "ruta": "/api/peliculas", "timestamp": "2026-11-17T20:00:33.0178777"}
```

---

## 9. CORS para Angular 21

Configuración centralizada en `config/CorsConfig.java`:

| Elemento | Valor |
|---|---|
| Origen predeterminado | `http://localhost:4200` |
| Variable de entorno | `APP_CORS_ALLOWED_ORIGINS` (varios orígenes separados por coma) |
| Rutas | `/api/**` |
| Métodos | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Encabezados permitidos | `Content-Type`, `Accept`, `Origin`, `X-Requested-With` |
| Encabezado expuesto | `Location` |
| Credenciales | no habilitadas |
| Comodín `*` | no se usa por defecto |

Para un despliegue futuro:

```text
APP_CORS_ALLOWED_ORIGINS=https://taquilla.example.com,https://admin.example.com
```

Respuesta real de preflight:

```text
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: Content-Type
Access-Control-Expose-Headers: Location
Access-Control-Max-Age: 3600
```

Un origen no autorizado recibe `403` con cuerpo de texto `Invalid CORS request`
y **sin** `Access-Control-Allow-Origin`. El navegador bloquea la lectura de la
respuesta.

---

## 10. Reglas de negocio relevantes para el frontend

1. Una película con funciones no se puede eliminar (`409`).
2. Una función con compras no se puede eliminar (`409`).
3. Al crear una función, `cuposDisponibles = cuposTotales`.
4. `cuposDisponibles` es de solo lectura para el cliente.
5. Al actualizar la capacidad, se conservan los boletos vendidos y
   `cuposDisponibles = cuposTotales - vendidos`.
6. No se puede reducir `cuposTotales` por debajo de los boletos vendidos
   (`409`).
7. No se puede cambiar la película de una función con compras (`409`).
8. El total de una compra lo calcula el servidor:
   `precio × cantidadBoletos` con 2 decimales.
9. Una compra rechazada con `409` no modifica cupos ni crea registros.
10. Las compras concurrentes no generan sobreventa (bloqueo `FOR UPDATE`).
11. Los reportes usan los importes históricos de `compras.total`.
12. Los reportes agrupan los días según `America/Bogota`.

### Comportamiento ante `409`

- **`Cupos insuficientes`:** refrescar la función (`GET /api/funciones/{id}`) y
  mostrar el mensaje; no reintentar automáticamente la misma compra.
- **`Conflicto de integridad` al borrar:** informar que existen registros
  relacionados; no ofrecer eliminación en cascada.
- **`Conflicto de integridad` al actualizar capacidad:** consultar de nuevo la
  función y permitir corregir `cuposTotales`.
- El cuerpo del `409` siempre incluye `error` y `mensaje` seguros para mostrar
  al usuario.

---

## 11. Estructuras de datos

### `PeliculaRequest`

```json
{"titulo": "Interestelar", "genero": "Ciencia ficcion", "duracionMin": 169, "clasificacion": "PG-13"}
```

| Campo | Tipo | Obligatorio | Generado por el servidor |
|---|---|---|---|
| `titulo` | string | sí | no |
| `genero` | string | sí | no |
| `duracionMin` | number | sí | no |
| `clasificacion` | string | sí | no |

### `PeliculaResponse`

| Campo | Tipo | Generado |
|---|---|---|
| `id` | number | sí |
| `titulo` | string | no |
| `genero` | string | no |
| `duracionMin` | number | no |
| `clasificacion` | string | no |

### `FuncionRequest`

```json
{"peliculaId": 770, "fecha": "2026-12-01", "hora": "18:30:00", "sala": "Sala 1", "precio": 15000.00, "cuposTotales": 100}
```

| Campo | Tipo | Obligatorio | Generado |
|---|---|---|---|
| `peliculaId` | number | sí | no |
| `fecha` | string `YYYY-MM-DD` | sí | no |
| `hora` | string `HH:mm[:ss]` | sí | no |
| `sala` | string | sí | no |
| `precio` | number | sí | no |
| `cuposTotales` | number | sí | no |

`cuposDisponibles` no se acepta como entrada.

### `FuncionResponse`

| Campo | Tipo | Generado |
|---|---|---|
| `id` | number | sí |
| `peliculaId` | number | no |
| `tituloPelicula` | string | sí (JOIN) |
| `fecha` | string `YYYY-MM-DD` | no |
| `hora` | string `HH:mm:ss` | no |
| `sala` | string | no |
| `precio` | number | no |
| `cuposTotales` | number | no |
| `cuposDisponibles` | number | sí (calculado) |

### `CompraRequest`

```json
{"funcionId": 545, "nombreCliente": "Cliente E2E A", "cantidadBoletos": 3}
```

| Campo | Tipo | Obligatorio | Generado |
|---|---|---|---|
| `funcionId` | number | sí | no |
| `nombreCliente` | string | sí | no |
| `cantidadBoletos` | number | sí | no |

### `CompraResponse`

| Campo | Tipo | Generado |
|---|---|---|
| `id` | number | sí |
| `funcionId` | number | no |
| `nombreCliente` | string | no |
| `cantidadBoletos` | number | no |
| `total` | number | sí (calculado) |
| `fechaCompra` | string ISO-8601 con offset | sí |

### `DetalleVentaResponse`

| Campo | Tipo | Origen |
|---|---|---|
| `id` | number | `compras.id` |
| `funcionId` | number | `compras.funcion_id` |
| `tituloPelicula` | string | `peliculas.titulo` actual |
| `fechaFuncion` | string `YYYY-MM-DD` | `funciones.fecha` actual |
| `horaFuncion` | string `HH:mm:ss` | `funciones.hora` actual |
| `sala` | string | `funciones.sala` actual |
| `nombreCliente` | string | `compras.nombre_cliente` |
| `cantidadBoletos` | number | `compras.cantidad_boletos` |
| `total` | number | `compras.total` histórico |
| `fechaCompra` | string ISO-8601 `-05:00` | `compras.fecha_compra` |

### `ReporteVentasResponse`

| Campo | Tipo |
|---|---|
| `fechaDesde` | string `YYYY-MM-DD` |
| `fechaHasta` | string `YYYY-MM-DD` |
| `cantidadCompras` | number (entero) |
| `totalBoletos` | number (entero) |
| `totalRecaudado` | number (2 decimales) |
| `compras` | `DetalleVentaResponse[]` |

### `ErrorResponse` y `ValidationErrorResponse`

| Campo | Tipo | Presente |
|---|---|---|
| `status` | number | siempre |
| `error` | string | siempre |
| `mensaje` | string | siempre |
| `ruta` | string | siempre |
| `timestamp` | string | siempre |
| `errores` | `Record<string, string>` | solo en errores de validación `400` |

---

## 12. Interfaces TypeScript recomendadas

> Estas interfaces son una recomendación para la futura fase del frontend. En la
> fase 2.6 **no** se creó ningún archivo TypeScript ni el proyecto Angular.

```ts
/** Identificadores BIGINT. Seguros como number en el rango de este proyecto. */
export type Id = number;

export interface PeliculaRequest {
  titulo: string;          // requerido, máx. 150
  genero: string;          // requerido, máx. 80
  duracionMin: number;     // requerido, >= 1
  clasificacion: string;   // requerido, máx. 20
}

export interface PeliculaResponse extends PeliculaRequest {
  id: Id;
}

export interface FuncionRequest {
  peliculaId: Id;          // requerido, > 0
  fecha: string;           // 'YYYY-MM-DD'
  hora: string;            // 'HH:mm' o 'HH:mm:ss'
  sala: string;            // requerido, máx. 50
  precio: number;          // requerido, > 0, 2 decimales
  cuposTotales: number;    // requerido, >= 1
}

export interface FuncionResponse {
  id: Id;
  peliculaId: Id;
  tituloPelicula: string;
  fecha: string;           // 'YYYY-MM-DD'
  hora: string;            // 'HH:mm:ss'
  sala: string;
  precio: number;
  cuposTotales: number;
  cuposDisponibles: number;
}

export interface CompraRequest {
  funcionId: Id;           // requerido, > 0
  nombreCliente: string;   // requerido, máx. 150
  cantidadBoletos: number; // requerido, >= 1
}

export interface CompraResponse {
  id: Id;
  funcionId: Id;
  nombreCliente: string;
  cantidadBoletos: number;
  total: number;
  fechaCompra: string;     // ISO-8601 con offset
}

export interface DetalleVentaResponse {
  id: Id;
  funcionId: Id;
  tituloPelicula: string;
  fechaFuncion: string;    // 'YYYY-MM-DD'
  horaFuncion: string;     // 'HH:mm:ss'
  sala: string;
  nombreCliente: string;
  cantidadBoletos: number;
  total: number;
  fechaCompra: string;     // ISO-8601 con offset '-05:00'
}

export interface ReporteVentasResponse {
  fechaDesde: string;      // 'YYYY-MM-DD'
  fechaHasta: string;      // 'YYYY-MM-DD'
  cantidadCompras: number;
  totalBoletos: number;
  totalRecaudado: number;
  compras: DetalleVentaResponse[];
}

export interface ErrorResponse {
  status: number;
  error: string;
  mensaje: string;
  ruta: string;
  timestamp: string;
}

export interface ValidationErrorResponse extends ErrorResponse {
  errores: Record<string, string>;
}
```

Recomendaciones adicionales:

- Tipar las respuestas de error como `ErrorResponse` o
  `ValidationErrorResponse` según el estado.
- Usar `HttpParams` para `fechaDesde` y `fechaHasta`; no construir la URL con
  concatenación manual.
- Enviar siempre `Content-Type: application/json` en `POST` y `PUT`.
- Para leer `Location`, usar `response.headers.get('Location')`; el backend lo
  expone mediante CORS.
- No enviar `cuposDisponibles` en `FuncionRequest`.
- Refrescar la función después de un `409` de cupos o capacidad.

---

## 13. Limitaciones conocidas del contrato

- No hay autenticación ni autorización.
- No hay paginación ni filtros en los listados.
- No hay consulta individual ni historial de compras.
- No hay endpoints de devolución o anulación.
- `PeliculaResponse` y `FuncionResponse` no incluyen fechas de auditoría.
- La información de película/función en el reporte refleja los datos actuales
  relacionados, no una copia histórica.
- El `Location` de `POST` es una URI relativa; Angular debe resolverla contra la
  URL base si necesita una URL absoluta.
