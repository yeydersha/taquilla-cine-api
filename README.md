# Taquilla de Cine API

Backend del sistema de taquilla de cine, desarrollado con **Java 21** y **Spring Boot 4.1.1**.

## Descripción

Expone una API REST para la gestión de películas, funciones, compras y reportes de taquilla. Se comunica con la base de datos PostgreSQL definida en el componente `taquilla-cine-db`.

## Tecnologías

| Tecnología | Versión |
|------------|---------|
| Java | 21 (LTS) |
| Spring Boot | 4.1.1 |
| Maven | 3.9.9 (Wrapper) |
| Spring Web | API REST |
| Spring JDBC / JdbcTemplate | Acceso a datos |
| PostgreSQL Driver | 42.7+ |
| Jakarta Bean Validation | 3.1 |
| Spring Boot Actuator | Health checks |
| Spring Boot Starter Test | Pruebas |

**Excluidos intencionalmente:** Hibernate/JPA, Lombok, frameworks adicionales.

## Arquitectura por Capas

```
src/main/java/com/taquillacine/api/
├── TaquillaCineApplication.java    # Punto de entrada
├── config/                         # Configuración CORS
├── controller/                     # Controladores REST (13 endpoints)
├── service/                        # Lógica de negocio y transacciones
├── repository/                     # Acceso a datos (JdbcTemplate)
├── model/                          # Entidades de dominio (mapeo 1:1 a tablas)
├── dto/                            # Data Transfer Objects (API)
└── exception/                      # Excepciones personalizadas
```

## Requisitos Previos

- Java 21 instalado y en PATH.
- PostgreSQL 12+ accesible (local o remoto).
- Variables de entorno configuradas (ver Configuración).

## Configuración

La aplicación utiliza variables de entorno (sin credenciales en código):

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `DB_URL` | URL JDBC | `jdbc:postgresql://localhost:5432/taquilla_cine_test` |
| `DB_USERNAME` | Usuario BD | `postgres` |
| `DB_PASSWORD` | Contraseña | (vacío) |
| `SERVER_PORT` | Puerto HTTP | `8080` |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes CORS | `http://localhost:4200` |

### Ejemplo `application-local.properties` (Neon, no versionado)

```properties
spring.datasource.url=jdbc:postgresql://TU_HOST_NEON:5432/taquilla_cine?sslmode=require
spring.datasource.username=TU_USUARIO_NEON
spring.datasource.password=***
```

## Separación Obligatoria de Entornos

| Entorno | Base de datos | Uso | Variables |
|---------|---------------|-----|-----------|
| **Local (automatizado)** | `taquilla_cine_test` (PostgreSQL local, 5432) | **Solo pruebas automatizadas** (`mvn test`) | `DB_URL=jdbc:postgresql://localhost:5432/taquilla_cine_test` |
| **Remoto / Usuario (Neon)** | `taquilla_cine` en **Neon** | **Ejecución habitual, desarrollo, pruebas manuales** | Configurado en `application-local.properties` (no versionado) |

**Reglas:**
- ✅ Neon para ejecución habitual y pruebas manuales.
- ✅ PostgreSQL local (`taquilla_cine_test`) **exclusivamente** para `mvn test`.
- ❌ No mezclar datos entre entornos. No modificar credenciales de Neon.

## Ejecución

### Compilar
```bash
./mvnw.cmd compile
```

### Ejecutar pruebas (requiere BD local `taquilla_cine_test`)
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_contraseña_local"
.\mvnw.cmd test
```
> **Guarda de entorno:** `GuardiaBaseDatosPruebas` verifica `current_database() = 'taquilla_cine_test'` y host local. Si falla, las pruebas no crean datos.

### Empaquetar JAR
```bash
./mvnw.cmd package
java -jar target/api-0.0.1-SNAPSHOT.jar
```

### Desarrollo / Usuario (Neon)
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./application-local.properties"
```
Verificación: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

El script `iniciar.bat` (en la raíz) automatiza el arranque del backend (Neon) y el frontend Angular.

## Endpoints REST (13 + Health)

| # | Método | Ruta | Éxito | Descripción |
|---|--------|------|-------|-------------|
| 1 | GET | `/api/peliculas` | 200 | Listar películas |
| 2 | GET | `/api/peliculas/{id}` | 200 | Consultar película |
| 3 | POST | `/api/peliculas` | 201 | Crear película |
| 4 | PUT | `/api/peliculas/{id}` | 200 | Actualizar película |
| 5 | DELETE | `/api/peliculas/{id}` | 204 | Eliminar película sin funciones |
| 6 | GET | `/api/funciones` | 200 | Listar funciones |
| 7 | GET | `/api/funciones/{id}` | 200 | Consultar función |
| 8 | POST | `/api/funciones` | 201 | Programar función |
| 9 | PUT | `/api/funciones/{id}` | 200 | Actualizar función |
| 10 | DELETE | `/api/funciones/{id}` | 204 | Eliminar función sin compras |
| 11 | GET | `/api/compras` | 200 | Listar compras |
| 12 | POST | `/api/compras` | 201 | Registrar compra |
| 13 | GET | `/api/reportes/ventas` | 200 | Reporte de ventas por rango |
| — | GET | `/actuator/health` | 200 | Estado del backend |

**No existen:** `PUT`/`PATCH`/`DELETE` para compras, ni `GET /api/compras/{id}`. Sin paginación ni filtros configurables.

Contrato completo (campos, tipos, límites, formatos, interfaces TS): [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md).

## Reglas de Negocio Principales

- **Películas:** CRUD completo. Eliminación bloqueada si tiene funciones (`409`).
- **Funciones:** CRUD completo. `cuposDisponibles` se inicializa igual a `cuposTotales` y se descuenta al registrar compras. Reducción de capacidad permitida solo si `nuevaCapacidad ≥ cuposVendidos`. Eliminación bloqueada si tiene compras (`409`). Concurrencia con `SELECT ... FOR UPDATE`.
- **Compras:** Solo `GET` (listado) y `POST` (registro). Transacción única: `SELECT ... FOR UPDATE` sobre la función → valida cupos → inserta compra → descuenta cupos → commit. Atomicidad garantizada. `409` si cupos insuficientes (sin cambios). `total = precio × cantidad` calculado en servidor (`BigDecimal`).
- **Reportes:** Solo lectura. Filtro por rango de fechas en zona `America/Bogota`. Indicadores (`cantidadCompras`, `totalBoletos`, `totalRecaudado`) calculados sobre la misma lista → consistencia garantizada.

## Compilación y Pruebas

```bash
# Compilar
./mvnw.cmd compile

# Ejecutar pruebas (requiere BD local taquilla_cine_test)
$env:DB_URL="jdbc:postgresql://localhost:5432/taquilla_cine_test"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_contraseña_local"
./mvnw.cmd test
```

### Resumen de Pruebas Automatizadas (142/142 ✅)

| Módulo | Pruebas | Cobertura |
|--------|---------|-----------|
| Películas | 25 | CRUD, validaciones, 409, regresión orden |
| Funciones | 36 | CRUD, cupos, concurrencia, 409 |
| Compras | 31 HTTP + 7 concurrencia + 1 atomicidad | Registro, cupos, 409, rollback |
| Reportes | 26 | Rangos, zona horaria, consistencia, 400 |
| Auditoría API | 16 | CORS, errores, health, contrato |

**Total: 142 pruebas, 0 fallidas, 0 errores, 0 omitidas.**

## Limitaciones Funcionales

- No hay autenticación ni autorización.
- No hay paginación, filtros ni ordenamiento configurable en listados.
- No hay devoluciones, modificaciones ni eliminación de compras.
- No hay exportación de reportes (Excel/PDF).
- No hay reglas de fechas futuras, cruce de horarios ni disponibilidad de salas.
- Esquema administrado exclusivamente por `taquilla-cine-db` (`spring.sql.init.mode=never`).

## Repositorios Relacionados

- **Base de Datos (PostgreSQL)**: https://github.com/yeydersha/taquilla-cine-db
- **Frontend (Angular 21)**: https://github.com/yeydersha/taquilla-cine-web

## Licencia

Proyecto académico — Taquilla de Cine.