# Taquilla de Cine API

Backend del sistema de taquilla de cine, desarrollado con Java 21 y Spring Boot 4.1.1.

## Descripción

Este componente expone una API REST para la gestión de películas, funciones y compras de entradas. Se comunica con la base de datos PostgreSQL definida en el componente `taquilla-cine-db`.

**Fase actual: 4.2 - Integración con PostgreSQL Remoto (Neon) completada**

## Tecnologías

- **Java 21** (LTS)
- **Spring Boot 4.1.1**
- **Maven 3.9.9** (via Maven Wrapper)
- **Spring Web** - API REST
- **Spring JDBC / JdbcTemplate** - Acceso a base de datos
- **PostgreSQL Driver 42.7+**
- **Jakarta Bean Validation 3.1** - Validación de DTOs
- **Spring Boot Actuator** - Health checks y monitoreo
- **Spring Boot Starter Test** - Pruebas unitarias e integración

**Excluidos intencionalmente:** Hibernate/JPA, Lombok, frameworks adicionales.

## Requisitos previos

- Java 21 instalado y en PATH
- PostgreSQL 12+ accesible (local o remoto)
- Variables de entorno configuradas (ver sección Configuración)

## Arquitectura del proyecto

```
taquilla-cine-api/
├── src/main/java/com/taquillacine/api/
│   ├── TaquillaCineApplication.java    # Punto de entrada
│   ├── config/                         # Configuración CORS
│   ├── controller/                     # Controladores REST
│   ├── service/                        # Lógica de negocio
│   ├── repository/                     # Acceso a datos (JdbcTemplate)
│   ├── model/                          # Entidades de dominio (mapeo 1:1 a tablas)
│   ├── dto/                            # Data Transfer Objects (API)
│   └── exception/                      # Excepciones personalizadas
├── src/main/resources/
│   └── application.properties          # Configuración (con placeholders para env vars)
├── src/test/java/com/taquillacine/api/
│   ├── PeliculaControllerIntegrationTest.java       # Pruebas de películas
│   ├── FuncionControllerIntegrationTest.java        # Pruebas de funciones
│   ├── CompraControllerIntegrationTest.java         # Pruebas HTTP de compras
│   ├── CompraConcurrenciaIntegrationTest.java       # Pruebas de concurrencia
│   ├── CompraAtomicidadIntegrationTest.java         # Prueba de rollback
│   ├── ReporteControllerIntegrationTest.java        # Pruebas del reporte de ventas
│   ├── AuditoriaApiIntegrationTest.java             # CORS, errores y contrato (fase 2.6)
│   └── support/                                     # Base de pruebas y guardia de entorno
├── docs/
│   └── API_CONTRACT.md                 # Contrato de integración para Angular 21
├── pom.xml                             # Configuración Maven
├── mvnw / mvnw.cmd                     # Maven Wrapper
├── .mvn/wrapper/                       # Wrapper properties + jar
├── .env.example                        # Ejemplo de variables de entorno
└── .gitignore                          # Archivos ignorados por Git
```

Paquete base: `com.taquillacine.api`

## Configuración de variables de entorno

La aplicación utiliza las siguientes variables de entorno:

| Variable | Descripción | Valor predeterminado |
|----------|-------------|----------------------|
| `DB_URL` | URL JDBC de la base de datos | `jdbc:postgresql://localhost:5432/taquilla_cine_test` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña del usuario | vacío |
| `SERVER_PORT` | Puerto HTTP del backend | `8080` |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes autorizados para CORS, separados por coma | `http://localhost:4200` |

Todas son opcionales: los valores predeterminados permiten ejecutar el proyecto en
desarrollo local. No hay contraseñas ni credenciales escritas en el código.

### En PowerShell (Windows)

```powershell
# Temporal (solo sesión actual)
$env:DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_contraseña_segura"
$env:SERVER_PORT="8080"
$env:APP_CORS_ALLOWED_ORIGINS="http://localhost:4200"

# Permanente (usuario actual)
[Environment]::SetEnvironmentVariable("DB_URL", "jdbc:postgresql://localhost:5432/taquilla_cine_test", "User")
[Environment]::SetEnvironmentVariable("DB_USERNAME", "postgres", "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", "tu_contraseña_segura", "User")
[Environment]::SetEnvironmentVariable("SERVER_PORT", "8080", "User")
[Environment]::SetEnvironmentVariable("APP_CORS_ALLOWED_ORIGINS", "http://localhost:4200", "User")
```

### En Linux/macOS (bash)

```bash
export DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
export DB_USERNAME="postgres"
export DB_PASSWORD="tu_contraseña_segura"
export SERVER_PORT="8080"
export APP_CORS_ALLOWED_ORIGINS="http://localhost:4200"
```

### Archivo `.env.example`

El repositorio incluye un archivo `.env.example` con valores de referencia. **Spring Boot no carga archivos `.env` automáticamente**; úsalo solo como referencia para configurar las variables en tu entorno.

> **Nota:** Para conexiones remotas con SSL, añade `?sslmode=require` a la URL JDBC:
> `jdbc:postgresql://host:5432/db?sslmode=require`

> **Esquema:** la aplicación tiene `spring.sql.init.mode=never`. No crea ni
> modifica tablas automáticamente; el esquema se administra únicamente con los
> scripts de `taquilla-cine-db`.

## Configuración CORS para Angular 21

La configuración está centralizada en `src/main/java/com/taquillacine/api/config/CorsConfig.java`
(Spring MVC, `WebMvcConfigurer.addCorsMappings`).

| Elemento | Valor |
|---|---|
| Origen predeterminado | `http://localhost:4200` |
| Variable de entorno | `APP_CORS_ALLOWED_ORIGINS` |
| Rutas | `/api/**` |
| Métodos | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Encabezados permitidos | `Content-Type`, `Accept`, `Origin`, `X-Requested-With` |
| Encabezado expuesto | `Location` |
| Credenciales | no habilitadas |
| Comodín `*` | no se usa por defecto |

Para autorizar varios orígenes en un despliegue:

```text
APP_CORS_ALLOWED_ORIGINS=https://taquilla.example.com,https://admin.example.com
```

El endpoint `/actuator/health` no está bajo `/api/**` y no tiene CORS. El rechazo
de un origen no autorizado devuelve `403` sin `Access-Control-Allow-Origin`.

## Seguridad y errores

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `SERVER_PORT` son configurables.
- `Actuator` solo expone `health` y sin detalles internos.
- Las respuestas de error usan un formato JSON estable y no incluyen SQL,
  stacktraces, nombres de restricciones ni credenciales.
- Solo las violaciones de clave foránea (`SQLSTATE 23503`) se traducen a `409`;
  otras violaciones de integridad responden `500` con un mensaje genérico.
- `.env`, `target/`, logs y archivos de IDE están excluidos por `.gitignore`.

## Contrato de integración con Angular 21

El contrato completo (endpoints, campos, tipos, límites, formatos, errores,
reglas de negocio e interfaces TypeScript recomendadas) está en:

[`docs/API_CONTRACT.md`](docs/API_CONTRACT.md)

## Comandos

### Compilar
```bash
./mvnw.cmd compile
```

### Ejecutar pruebas
```bash
./mvnw.cmd test
```

### Ejecutar la aplicación
```bash
./mvnw.cmd spring-boot:run
```
El servidor inicia en `http://localhost:8080`. Para usar otro puerto, define
`SERVER_PORT` antes de arrancar (por ejemplo, `SERVER_PORT=9090`).

### Empaquetar (JAR ejecutable)
```bash
./mvnw.cmd package
java -jar target/api-0.0.1-SNAPSHOT.jar
```

## Configuración de PostgreSQL

### Separación obligatoria de entornos

Este proyecto mantiene **dos entornos de base de datos completamente separados** que **nunca deben mezclarse**:

| Entorno | Base de datos | Uso | Variables |
|---------|---------------|-----|-----------|
| **Local (automatizado)** | `taquilla_cine_test` (PostgreSQL local, puerto 5432) | **Exclusivamente para pruebas automatizadas** (`mvn test`) | `DB_URL=jdbc:postgresql://localhost:5432/taquilla_cine_test` |
| **Remoto (producción/usuario)** | `taquilla_cine` en **Neon** | **Ejecución habitual, pruebas manuales, desarrollo del usuario** | Configurado en `application-local.properties` (no versionado) |

**Reglas estrictas:**
- ✅ **Neon** para la ejecución habitual del sistema y pruebas manuales del usuario.
- ✅ **PostgreSQL local (`taquilla_cine_test`)** exclusivamente para pruebas automatizadas (`mvn test`).
- ✅ Después de cualquier ejecución de pruebas automatizadas, si se reinicia la aplicación para el usuario, **hacerlo conectado a Neon**.
- ❌ No modificar ni mostrar las credenciales de Neon.
- ❌ No copiar datos entre ambas bases de datos.
- ❌ No eliminar registros de ninguna base.
- ❌ No modificar la lógica de negocio ni el esquema SQL.

El archivo `application-local.properties` (en la raíz del proyecto, **no versionado en Git**) contiene la configuración de Neon y se carga mediante:
```bash
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./application-local.properties"
```

El script `iniciar.bat` ya incluye este comando para arrancar el backend conectado a Neon junto con el frontend Angular.

---

### Local (solo pruebas automatizadas)
1. Inicia el servicio PostgreSQL:
   ```powershell
   Start-Service postgresql-x64-17
   ```
2. Crea la base de datos de pruebas (si no existe):
   ```powershell
   psql -U postgres -c "CREATE DATABASE taquilla_cine_test;"
   ```
3. Ejecuta los scripts SQL del componente `taquilla-cine-db` en orden:
   ```powershell
   psql -U postgres -d taquilla_cine_test -f ..\taquilla-cine-db\sql\01_create_tables.sql
   psql -U postgres -d taquilla_cine_test -f ..\taquilla-cine-db\sql\02_constraints_indexes.sql
   ```
4. Define variables de entorno para las pruebas:
   ```powershell
   $env:DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
   $env:DB_USERNAME="postgres"
   $env:DB_PASSWORD="tu_contraseña_local"
   ```
5. Ejecuta las pruebas:
   ```powershell
   .\mvnw.cmd test
   ```

> **Guarda de entorno:** Las pruebas de integración incluyen `GuardiaBaseDatosPruebas` que verifica `current_database() = 'taquilla_cine_test'` y que el host sea local (`local`, `127.0.0.1`, `::1`). Si no se cumple, las pruebas fallan inmediatamente sin crear datos.

### Remota / Usuario (Neon - funcionamiento habitual)
- La configuración está en `application-local.properties` (archivo privado, no en Git).
- Inicio recomendado (ya incluido en `iniciar.bat`):
  ```powershell
  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./application-local.properties"
  ```
- Verificación de salud:
  ```powershell
  curl.exe http://localhost:8080/actuator/health
  # Respuesta esperada: {"groups":["liveness","readiness"],"status":"UP"}
  ```

## Módulo de Películas (Fase 2.2)

### Endpoints implementados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/peliculas` | Listar todas las películas |
| GET | `/api/peliculas/{id}` | Consultar película por ID |
| POST | `/api/peliculas` | Crear nueva película |
| PUT | `/api/peliculas/{id}` | Actualizar película existente |
| DELETE | `/api/peliculas/{id}` | Eliminar película |

### Códigos de estado HTTP

| Código | Descripción |
|--------|-------------|
| 200 | Consulta o actualización exitosa |
| 201 | Película creada (con header `Location`) |
| 204 | Película eliminada correctamente |
| 400 | Datos de entrada inválidos (validación) |
| 404 | Película no encontrada |
| 409 | Conflicto de integridad referencial (ej. película con funciones asociadas) |
| 500 | Error interno inesperado |

### Ejemplo de solicitud (POST / PUT)

```json
{
  "titulo": "Interestelar",
  "genero": "Ciencia ficción",
  "duracionMin": 169,
  "clasificacion": "PG-13"
}
```

### Ejemplo de respuesta (GET / POST / PUT)

```json
{
  "id": 1,
  "titulo": "Interestelar",
  "genero": "Ciencia ficción",
  "duracionMin": 169,
  "clasificacion": "PG-13"
}
```

### Validaciones de entrada

| Campo | Reglas |
|-------|--------|
| `titulo` | Obligatorio, no vacío, máx. 150 caracteres |
| `genero` | Obligatorio, no vacío, máx. 80 caracteres |
| `duracionMin` | Obligatorio, entero > 0 |
| `clasificacion` | Obligatorio, no vacío, máx. 20 caracteres |

### Manejo de errores

Formato JSON consistente para todos los errores:

```json
{
  "status": 400,
  "error": "Datos de entrada inválidos",
  "mensaje": "Validación fallida en uno o más campos",
  "ruta": "/api/peliculas",
  "timestamp": "2026-11-17T18:30:00",
  "errores": {
    "titulo": "El título es obligatorio",
    "duracionMin": "La duración debe ser mayor que cero"
  }
}
```

Para errores de conflicto (409):
```json
{
  "status": 409,
  "error": "Conflicto de integridad",
  "mensaje": "No se puede eliminar la película porque tiene funciones asociadas",
  "ruta": "/api/peliculas/1",
  "timestamp": "2026-11-17T18:30:00"
}
```

## Módulo de Funciones (Fase 2.3)

### Endpoints implementados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/funciones` | Listar funciones con el título de la película |
| GET | `/api/funciones/{id}` | Consultar función por ID |
| POST | `/api/funciones` | Crear nueva función |
| PUT | `/api/funciones/{id}` | Actualizar función existente |
| DELETE | `/api/funciones/{id}` | Eliminar función |

Las respuestas incluyen `tituloPelicula`, obtenido mediante `JOIN` con la tabla `peliculas`.
El listado se ordena de forma estable por `fecha`, `hora` e `id` ascendentes.

### Ejemplo de solicitud (POST / PUT)

```json
{
  "peliculaId": 1,
  "fecha": "2026-12-01",
  "hora": "18:30:00",
  "sala": "Sala 1",
  "precio": 15000.00,
  "cuposTotales": 100
}
```

`cuposDisponibles` no forma parte del contrato de entrada: el cliente no puede establecerlo
ni modificarlo directamente.

### Ejemplo de respuesta (GET / POST / PUT)

```json
{
  "id": 1,
  "peliculaId": 1,
  "tituloPelicula": "Interestelar",
  "fecha": "2026-12-01",
  "hora": "18:30:00",
  "sala": "Sala 1",
  "precio": 15000.00,
  "cuposTotales": 100,
  "cuposDisponibles": 100
}
```

### Validaciones de entrada

| Campo | Reglas |
|-------|--------|
| `peliculaId` | Obligatorio, entero positivo y existente en `peliculas` |
| `fecha` | Obligatoria (`YYYY-MM-DD`) |
| `hora` | Obligatoria (`HH:MM:SS`) |
| `sala` | Obligatoria, no vacía, máx. 50 caracteres |
| `precio` | Obligatorio, mayor que cero, compatible con `NUMERIC(12,2)` |
| `cuposTotales` | Obligatorio, entero mayor que cero |

No se aplican reglas de fechas futuras, salas ni cruces de horarios, porque no fueron aprobadas.

### Reglas de administración de cupos

1. **Creación:** `cuposDisponibles` se inicializa automáticamente igual a `cuposTotales`.
2. **Cupos vendidos:** se calculan como `cuposTotales - cuposDisponibles` de la función actual.
3. **Aumento de capacidad:** los cupos disponibles aumentan en la misma proporción, conservando las ventas.
4. **Reducción de capacidad:** se permite solo si la nueva capacidad total no es menor que los cupos
   ya vendidos. Los nuevos cupos disponibles se calculan como
   `nuevaCapacidadTotal - cuposVendidos`.
5. **Película con compras:** si la función tiene compras registradas, no se permite cambiar su película.
6. **Eliminación:** una función con compras asociadas no puede eliminarse (`ON DELETE RESTRICT`).
7. **Concurrencia:** las actualizaciones y eliminaciones bloquean la fila con `SELECT ... FOR UPDATE`
   dentro de una transacción. El módulo de compras usa el mismo bloqueo, por lo que compras,
   actualizaciones y eliminaciones concurrentes sobre una función se serializan.

### Códigos de estado HTTP

| Código | Descripción |
|--------|-------------|
| 200 | Consulta o actualización exitosa |
| 201 | Función creada (con header `Location`) |
| 204 | Función eliminada correctamente |
| 400 | Datos de entrada inválidos (validación o JSON malformado) |
| 404 | Función o película no encontrada |
| 409 | Conflicto de negocio o de integridad referencial |
| 500 | Error interno inesperado |

### Manejo de conflictos (409)

- Reducir `cuposTotales` por debajo de los cupos vendidos.
- Cambiar la película de una función que ya tiene compras.
- Eliminar una función con compras asociadas.

```json
{
  "status": 409,
  "error": "Conflicto de integridad",
  "mensaje": "La nueva capacidad total (9) no puede ser inferior a los cupos ya vendidos (10)",
  "ruta": "/api/funciones/1",
  "timestamp": "2026-11-17T18:30:00"
}
```

> **Compras:** el módulo de compras se implementa en la fase 2.4 (ver la sección siguiente).
> Las pruebas de funciones usan registros de compras controlados para validar las reglas de
> cupos y los conflictos `409`.

## Módulo de Compras (Fase 2.4)

### Descripción

Registra la venta de boletos para una función. Cada compra es inmutable desde la API:
**no existen devoluciones, modificaciones ni eliminaciones**. Por eso solo se exponen el
listado y el registro. El servidor controla por completo el `id`, el `total`, la
`fechaCompra` y el descuento de cupos; el cliente únicamente envía `funcionId`,
`nombreCliente` y `cantidadBoletos`.

### Endpoints implementados

| Método | Endpoint | Descripción | Éxito |
|--------|----------|-------------|-------|
| GET | `/api/compras` | Listar compras, de la más reciente a la más antigua | 200 |
| POST | `/api/compras` | Registrar una compra | 201 |

No existen `PUT`, `PATCH`, `DELETE` ni `GET /api/compras/{id}`. Cualquier intento con
`PUT`, `PATCH` o `DELETE` responde `405 Method Not Allowed`.

El listado se ordena por `fecha_compra DESC, id DESC`. El ID actúa como criterio secundario
para que el orden sea estable cuando dos compras comparten la misma marca de tiempo.

### Ejemplo de solicitud

```json
{
  "funcionId": 1,
  "nombreCliente": "Carlos Pérez",
  "cantidadBoletos": 3
}
```

### Ejemplo de respuesta (201 Created)

```json
{
  "id": 1,
  "funcionId": 1,
  "nombreCliente": "Carlos Pérez",
  "cantidadBoletos": 3,
  "total": 45000.00,
  "fechaCompra": "2026-11-17T19:13:19.039032-05:00"
}
```

`fechaCompra` se serializa como ISO 8601 con desplazamiento horario. PostgreSQL la almacena
como `TIMESTAMPTZ`, por lo que al leerla puede representarse en UTC (`...Z`); en ambos casos
se trata del mismo instante.

### Validaciones de entrada

| Campo | Reglas |
|-------|--------|
| `funcionId` | Obligatorio, entero positivo y existente en `funciones` |
| `nombreCliente` | Obligatorio, no vacío, máx. 150 caracteres |
| `cantidadBoletos` | Obligatorio, entero mayor que cero |

Los campos `id`, `total`, `fechaCompra` y `cuposDisponibles` no forman parte del DTO de
entrada. Si el cliente los envía, se ignoran y el servidor los calcula o genera.

### Reglas de negocio

1. La compra se ejecuta completamente dentro de una única transacción (`@Transactional`).
2. La función se consulta con `SELECT ... FOR UPDATE`, que bloquea su fila hasta el commit.
3. Si la función no existe, se responde `404 Not Found`.
4. Se comparan los boletos solicitados con `cuposDisponibles` de la fila bloqueada.
5. Si la cantidad supera la disponibilidad, se responde `409 Conflict` sin escribir nada.
6. El total se calcula como `precio × cantidad` usando `BigDecimal`, con escala 2.
7. Si el total excede `NUMERIC(12,2)` (máximo `9999999999.99`), se rechaza con `400` antes
   de llegar a PostgreSQL.
8. La compra se inserta con `fechaCompra` generada por el servidor (truncada a microsegundos).
9. Se descuenta `cantidadBoletos` de `cuposDisponibles`.
10. Se verifica que el `UPDATE` afecte exactamente una fila; si no, se lanza una excepción.
11. Cualquier fallo revierte la transacción completa: no quedan compras parciales ni cupos
    descontados.

### Concurrencia y atomicidad

- `SELECT ... FOR UPDATE` serializa las compras concurrentes sobre la misma función. Dos
  clientes que intentan comprar más boletos de los disponibles no pueden sobrevender: una
  compra se confirma y la otra recibe `409`.
- El mismo bloqueo se usa en `FuncionService` al actualizar y eliminar funciones, por lo que
  una compra concurrente con una actualización o eliminación también queda serializada.
- `descontarCupos` incluye la condición `cupos_disponibles >= cantidad` como defensa
  adicional; la base nunca queda con cupos negativos.
- No se usa `synchronized` ni estado en memoria: el control es de PostgreSQL y funciona con
  varias instancias del backend.
- Las pruebas de concurrencia usan hilos, conexiones y transacciones independientes, con
  sincronización de inicio y timeouts acotados.
- La prueba de atomicidad usa una subclase de repositorio exclusiva del entorno de pruebas
  que inserta la compra y luego falla; se verifica que la compra y el descuento se reviertan.

### Cupos insuficientes (409)

```json
{
  "status": 409,
  "error": "Cupos insuficientes",
  "mensaje": "Se solicitaron 200 boletos, pero solo hay 95 disponibles",
  "ruta": "/api/compras",
  "timestamp": "2026-11-17T19:13:19.1625284"
}
```

La compra rechazada no modifica los cupos ni genera registros.

### Total fuera de rango (400)

```json
{
  "status": 400,
  "error": "Monto fuera de rango",
  "mensaje": "El total calculado (19999999999.98) excede el máximo permitido por la base de datos (9999999999.99)",
  "ruta": "/api/compras",
  "timestamp": "2026-11-17T19:13:19.1625284"
}
```

### Códigos de estado HTTP

| Código | Descripción |
|--------|-------------|
| 200 | Listado exitoso |
| 201 | Compra registrada |
| 400 | Datos inválidos, JSON malformado o total fuera de rango |
| 404 | Función inexistente |
| 405 | Método no permitido (`PUT`, `PATCH`, `DELETE`) |
| 409 | Cupos insuficientes |
| 500 | Error interno inesperado |

## Módulo de Reportes (Fase 2.5)

### Descripción

Expone un reporte de ventas de **solo lectura** para la cajera y para el futuro módulo de
taquilla del frontend Angular 21. Permite consultar las ventas de un día específico o de un
rango de fechas, junto con los indicadores del período.

El módulo no crea, actualiza ni elimina registros. Tampoco modifica el esquema PostgreSQL.

### Endpoint implementado

| Método | Endpoint | Descripción | Éxito |
|--------|----------|-------------|-------|
| GET | `/api/reportes/ventas` | Reporte de ventas por día o rango | 200 |

### Parámetros

| Parámetro | Obligatorio | Formato | Descripción |
|-----------|-------------|---------|-------------|
| `fechaDesde` | Sí | `YYYY-MM-DD` | Primer día incluido en el reporte |
| `fechaHasta` | Sí | `YYYY-MM-DD` | Último día incluido en el reporte |

`fechaDesde` debe ser menor o igual que `fechaHasta`. No se imponen límites de rango,
antigüedad ni fechas futuras.

### Ejemplos de solicitud

Un día específico:

```http
GET http://localhost:8080/api/reportes/ventas?fechaDesde=2026-11-17&fechaHasta=2026-11-17
```

Un rango de fechas:

```http
GET http://localhost:8080/api/reportes/ventas?fechaDesde=2026-11-01&fechaHasta=2026-11-30
```

### Ejemplo de respuesta (200 OK)

```json
{
  "fechaDesde": "2026-11-17",
  "fechaHasta": "2026-11-17",
  "cantidadCompras": 5,
  "totalBoletos": 8,
  "totalRecaudado": 125000.00,
  "compras": [
    {
      "id": 364,
      "funcionId": 319,
      "tituloPelicula": "Interestelar",
      "fechaFuncion": "2026-12-01",
      "horaFuncion": "18:30:00",
      "sala": "Sala 1",
      "nombreCliente": "Límite Fin",
      "cantidadBoletos": 1,
      "total": 15000.00,
      "fechaCompra": "2026-11-17T23:59:59-05:00"
    },
    {
      "id": 365,
      "funcionId": 319,
      "tituloPelicula": "Interestelar",
      "fechaFuncion": "2026-12-01",
      "horaFuncion": "18:30:00",
      "sala": "Sala 1",
      "nombreCliente": "Equivalente UTC",
      "cantidadBoletos": 1,
      "total": 20000.00,
      "fechaCompra": "2026-11-17T22:00:00-05:00"
    }
  ]
}
```

El ejemplo se abrevia: la respuesta real incluye todas las compras del período.

Un período sin ventas responde **200 OK**, con `cantidadCompras: 0`, `totalBoletos: 0`,
`totalRecaudado: 0.00` y `compras: []`. No se devuelve `404`.

### Indicadores

| Campo | Cálculo |
|-------|---------|
| `cantidadCompras` | Número de compras incluidas en el reporte |
| `totalBoletos` | Suma de `cantidadBoletos` de esas compras |
| `totalRecaudado` | Suma de `compras.total` de esas compras |

Los tres indicadores se calculan en el servicio a partir de **la misma lista de compras**
devuelta por una única consulta. Por lo tanto, siempre coinciden con el detalle, incluso si
otra transacción inserta compras mientras se genera el reporte. La lectura se ejecuta en una
transacción `@Transactional(readOnly = true)`.

`totalRecaudado` y el `total` de cada compra usan `BigDecimal`. Las cantidades usan `long`
para evitar desbordamientos al sumar. Los importes se toman del **total almacenado en
`compras`**, no del precio actual de la función: si el precio cambia después de la venta, el
reporte histórico conserva el importe original.

### Tratamiento de zonas horarias

`compras.fecha_compra` es `TIMESTAMPTZ`. El reporte interpreta los días en
**`America/Bogota`** (`ZoneId.of("America/Bogota")`), sin depender de la zona horaria del
servidor Java ni de la sesión de PostgreSQL.

El filtro es un intervalo semiabierto:

- Inicio: `fechaDesde` a las `00:00:00` en `America/Bogota`.
- Fin exclusivo: `fechaHasta + 1 día` a las `00:00:00` en `America/Bogota`.

```sql
WHERE c.fecha_compra >= ?
  AND c.fecha_compra < ?
```

No se usa `BETWEEN` con `23:59:59`, no se aplica `DATE()` sobre `fecha_compra` y los límites
se envían como parámetros JDBC. Así se conserva la semántica de instantes y se puede
aprovechar el índice existente sobre `fecha_compra`.

El filtro usa la fecha en que se realizó la compra, no la fecha programada de la función.
En la respuesta, `fechaCompra` se expresa con desplazamiento `-05:00`, por ejemplo
`2026-11-18T03:00:00Z` se presenta como `2026-11-17T22:00:00-05:00`.

### Consulta y orden

El repositorio ejecuta un `JOIN` entre `compras`, `funciones` y `peliculas` para obtener el
título, la fecha y hora de la función, la sala, el cliente, los boletos y el total. El orden
es `fecha_compra DESC, id DESC`, de modo que el `id` desempata de forma estable dos compras
con la misma marca de tiempo.

### Manejo de errores

| Código | Situación | `error` |
|--------|-----------|---------|
| 400 | Falta `fechaDesde` o `fechaHasta` | `Parámetro faltante` |
| 400 | Formato distinto a `YYYY-MM-DD` o fecha inexistente | `Parámetro inválido` |
| 400 | `fechaDesde` posterior a `fechaHasta` | `Rango de fechas inválido` |
| 500 | Error interno inesperado | `Error interno del servidor` |

Se conserva el formato global de errores:

```json
{
  "status": 400,
  "error": "Rango de fechas inválido",
  "mensaje": "La fecha inicial (2026-11-30) no puede ser posterior a la fecha final (2026-11-01)",
  "ruta": "/api/reportes/ventas",
  "timestamp": "2026-11-17T19:25:55.3097325"
}
```

### Consumo desde Angular 21

Este endpoint será consumido por el módulo de taquilla del frontend Angular 21 para mostrar
a la cajera las ventas del día o del rango seleccionado, junto con la cantidad de compras,
los boletos vendidos y el dinero recaudado. Los nombres de las propiedades se mantienen en
camelCase para facilitar el mapeo directo a interfaces TypeScript.

El contrato completo de los 13 endpoints funcionales, incluidos los campos exactos, los
tipos, los límites, los formatos y las interfaces TypeScript recomendadas, está en
[`docs/API_CONTRACT.md`](docs/API_CONTRACT.md). El frontend deberá consumir la API en
`http://localhost:8080` y su origen de desarrollo (`http://localhost:4200`) ya está
autorizado por CORS.

## Verificación de salud (Actuator)

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:
```json
{
  "groups": ["liveness", "readiness"],
  "status": "UP"
}
```

> La configuración `management.endpoint.health.show-details=never` evita exponer detalles sensibles.
> El endpoint responde con `Content-Type: application/vnd.spring-boot.actuator.v3+json`.

## Pruebas

```bash
# Ejecutar todas las pruebas (requiere variables de entorno DB_*)
$env:DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_contraseña"
./mvnw.cmd test
```

### Seguridad del entorno de pruebas

Antes de ejecutar cada prueba, `GuardiaBaseDatosPruebas` verifica:

- `current_database()` sea exactamente `taquilla_cine_test`.
- El host de conexión sea local (`local`, `127.0.0.1` o `::1`).

Si alguna condición no se cumple, las pruebas fallan de inmediato y no crean datos.

La limpieza **no usa `DELETE` masivos**. Cada prueba registra los IDs de las películas y
funciones que crea; las compras se eliminan por `funcion_id`. `PruebaIntegracionBase`
elimina únicamente esos registros en `@AfterEach`, en orden inverso a las dependencias:

```sql
DELETE FROM compras  WHERE funcion_id = ?;
DELETE FROM funciones WHERE id = ?;
DELETE FROM peliculas WHERE id = ?;
```

Las pruebas no dependen del orden de ejecución ni de que las tablas estén vacías.

### Cobertura de pruebas

**Películas: 25 pruebas de integración**

- Operaciones válidas: listar, crear, verificar persistencia, consultar, listar varias,
  actualizar, verificar persistencia de la actualización y eliminar.
- Operaciones inválidas: IDs inexistentes, campos obligatorios, longitudes máximas,
  duración inválida, JSON malformado y conflicto `409` por funciones asociadas.
- Regresión: ordenamiento por ID ascendente.

**Funciones: 36 pruebas de integración**

- Operaciones válidas: listar, crear para película existente, cupos iniciales,
  persistencia real, consultar por ID, listado múltiple y ordenado, `tituloPelicula`,
  actualizar, persistencia de la actualización, eliminar sin compras, aumentar capacidad
  y reducir capacidad respetando los cupos vendidos.
- Operaciones inválidas: película inexistente, función inexistente, validaciones de
  `peliculaId`, `fecha`, `hora`, `sala`, `precio`, `cuposTotales`, JSON malformado,
  reducción por debajo de los cupos vendidos, eliminación con compras, actualización con
  película inexistente, cambio de película con compras y manipulación de
  `cuposDisponibles`.
- Conflictos: `409` con verificación de que la función, los cupos y las compras
  permanecen intactos.

**Compras: 31 pruebas HTTP de integración**

- Operaciones válidas: listar sin registros, crear compra, `201`, ID automático, total
  exacto, fecha generada por el servidor, persistencia real, descuento de cupos, compras
  múltiples para una función, compra de todos los cupos, listado múltiple, orden estable y
  cálculo con decimales.
- Operaciones inválidas: función inexistente, `funcionId` ausente o negativo,
  `nombreCliente` ausente, vacío o mayor a 150 caracteres, `cantidadBoletos` ausente, cero o
  negativa, JSON malformado, cupos insuficientes, función agotada, compra rechazada sin
  cambios en cupos ni registros, manipulación de `total` y `fechaCompra`, total fuera de
  `NUMERIC(12,2)` y métodos `PUT`/`PATCH`/`DELETE` no permitidos.

**Compras: 7 pruebas de concurrencia**

- Dos compras cuya suma supera la disponibilidad.
- Dos compras que caben y se completan.
- Ráfaga de solicitudes hasta agotar los cupos.
- Cupos nunca negativos.
- Boletos vendidos iguales al descuento de cupos.
- Actualización concurrente de la capacidad.
- Eliminación concurrente de la función.

**Compras: 1 prueba de atomicidad**

- Fallo simulado después del `INSERT` y antes del descuento de cupos; se verifica el
  rollback completo de la compra y de los cupos.

**Reportes: 26 pruebas de integración**

- Período sin ventas (`200`, lista vacía y totales en cero).
- Ventas de un día y de varios días; `cantidadCompras`, `totalBoletos` y `totalRecaudado`.
- Precisión de importes decimales.
- Datos de películas y funciones obtenidos mediante `JOIN`.
- Orden descendente por `fecha_compra` e `id`.
- Exclusión de ventas anteriores y posteriores al rango.
- Límites: medianoche inicial incluida, último instante del día incluido y medianoche del
  día siguiente excluida.
- Diferencia entre UTC y `America/Bogota`.
- Compra con fecha de función distinta a la fecha de compra.
- Conservación del importe histórico al cambiar el precio de la función.
- Parámetros ausentes, formato inválido, fecha inexistente y rango invertido (`400`).
- Verificación de que el reporte no modifica datos.
- Consistencia de indicadores frente a inserciones concurrentes.

**Auditoría de API: 16 pruebas de integración (fase 2.6)**

- CORS: `GET` desde `http://localhost:4200`, preflight `OPTIONS`, rechazo de origen no
  autorizado (incluida una solicitud real que no crea datos), `POST` con `Location`
  expuesto, `PUT` y `DELETE`.
- `Content-Type` JSON en respuestas exitosas.
- Formato global de error de cinco campos, `400` por tipo de ruta inválido, `404` de ruta,
  `405` de método y `415` de tipo de contenido.
- Ausencia de SQL, trazas, nombres de restricciones y credenciales en los errores.
- `/actuator/health` disponible sin detalles internos.
- Conservación de los nueve campos de `FuncionResponse`.

### Ejecución completa (regresión)

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_contraseña"
.\mvnw.cmd test
```

Resultado verificado: **142 pruebas**, todas aprobadas, 0 fallidas, 0 errores, 0 omitidas.

## Restricciones del proyecto

- Las compras no admiten devoluciones, modificaciones ni eliminaciones mediante la API.
- El reporte de ventas es de solo lectura y no implementa devoluciones ni anulaciones.
- No se desarrolla el frontend Angular en este repositorio.
- No se modifican los scripts SQL existentes ni el esquema de la base de datos.
- No se modifica el componente de base de datos.
- No se modifican archivos fuera del proyecto.
- No se realizan commits, push ni despliegues sin autorización.
- No se instalan herramientas globales sin autorización.
- Se detienen los procesos iniciados durante las pruebas y se restaura el estado inicial de los servicios.

## Fase 4.2 - Integración con PostgreSQL Remoto (Neon) - Completada

**Estado:** ✅ Completada (14/14 comprobaciones manuales + 16/16 validación remota final aprobadas).

### Configuración Neon (backend)

Archivo `application-local.properties` (no versionado, creado por el usuario):
```properties
spring.datasource.url=jdbc:postgresql://TU_HOST_NEON:5432/taquilla_cine?sslmode=require
spring.datasource.username=TU_USUARIO_NEON
spring.datasource.password=***
```

### Arranque para desarrollo/usuario (Neon)

```powershell
cd ..\taquilla-cine-api
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./application-local.properties"
```

### Verificación de salud

```powershell
curl.exe http://localhost:8080/actuator/health
# Respuesta: {"groups":["liveness","readiness"],"status":"UP"}
```

### Separación obligatoria de entornos

| Entorno | Base de datos | Uso | Variables |
|---------|---------------|-----|-----------|
| **Local (automatizado)** | `taquilla_cine_test` (PostgreSQL local, puerto 5432) | **Exclusivamente para pruebas automatizadas** (`mvn test`) | `DB_URL=jdbc:postgresql://localhost:5432/taquilla_cine_test` |
| **Remoto / Usuario (Neon)** | `taquilla_cine` en **Neon** | **Ejecución habitual, pruebas manuales, desarrollo** | Configurado en `application-local.properties` (no versionado) |

**Reglas estrictas:**
- ✅ **Neon** para la ejecución habitual del sistema y pruebas manuales del usuario.
- ✅ **PostgreSQL local (`taquilla_cine_test`)** exclusivamente para pruebas automatizadas (`mvn test`).
- ✅ Después de cualquier ejecución de pruebas automatizadas, si se reinicia la aplicación para el usuario, **hacerlo conectado a Neon**.
- ❌ No modificar ni mostrar las credenciales de Neon.
- ❌ No copiar datos entre ambas bases de datos.
- ❌ No eliminar registros de ninguna base.
- ❌ No modificar la lógica de negocio ni el esquema SQL.

### Pruebas manuales con Neon (usuario)

| Categoría | Pruebas | Estado |
|-----------|---------|--------|
| Corrección selector compras | 14/14 | ✅ |
| Validación remota final (Neon) | 16/16 | ✅ |
| **Total fase 4.2** | **30/30** | **✅ Aprobado** |

### Resumen acumulado de pruebas

| Fase | Automatizadas | Manuales | Estado |
|------|---------------|----------|--------|
| Backend (mvn test) | 142/142 | — | ✅ |
| Frontend (npm run test:ci) | 96/96 | — | ✅ |
| Integración local (Fase 4.1) | — | 26/26 | ✅ |
| Integración Neon (Fase 4.2) | — | 30/30 | ✅ |
| **Total acumulado** | **238/238** | **56/56** | **✅** |

---

## Fase 5 - Preparación de Repositorios y Documentación Final

**Estado:** 🔄 En preparación.

### Documentación pendiente

- Actualizar `taquilla-cine-db/README.md`
- Actualizar `taquilla-cine-web/README.md` (ya actualizado)
- Actualizar `taquilla-cine-web/docs/PRUEBAS_INTEGRACION.md` con resultados finales

### Seguridad y repositorios

- Revisión de credenciales y `.gitignore`
- Preparación para commit inicial (sin push)
- Verificación de `.gitignore` y archivos excluidos

---

## Restricciones del proyecto

- Las compras no admiten devoluciones, modificaciones ni eliminaciones mediante la API.
- El reporte de ventas es de solo lectura y no implementa devoluciones ni anulaciones.
- No se desarrolla el frontend Angular en este repositorio.
- No se modifican los scripts SQL existentes ni el esquema de la base de datos.
- No se modifica el componente de base de datos.
- No se modifican archivos fuera del proyecto.
- No se realizan commits, push ni despliegues sin autorización.
- No se instalan herramientas globales sin autorización.
- Se detienen los procesos iniciados durante las pruebas y se restaura el estado inicial de los servicios.

## Licencia

Proyecto académico - Taquilla de Cine (Fase 4.2)