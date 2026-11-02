# Backend del Marketplace de Alojamientos — guía

API REST en Spring Boot 3.5 + Java 21 + PostgreSQL 17, con la estructura de código de la universidad
(`co.edu.unbosque`: `config`, `controller`, `dto`, `entity`, `repository`, `service/api`, `service/impl`, `utils`).

## 1. Cómo ejecutarlo

```bash
# 1. Base de datos (borra y crea marketplace_db y marketplace_test, usuario marketplace / MarketplaceLR2026)
psql postgres -f docs/base_de_datos.sql

# 2. Levantar la API (crea las tablas, las restricciones y los datos iniciales)
cd backend
./mvnw spring-boot:run

# 3. Pruebas automáticas (usan marketplace_test)
./mvnw test
```

Al iniciar se crean solos: las políticas FLEXIBLE (id 1) y ESTRICTA (id 2), 10 servicios (wifi, parqueadero...),
los parámetros de tarifas y el administrador `admin@marketplace.com` / `Admin2026*`.

Para probar con Postman: importa `docs/Marketplace_LR.postman_collection.json` y usa **Run collection**.
Las carpetas están en orden y guardan solas los tokens y los ids.

Documentación interactiva de la API (Swagger): **http://localhost:8080/swagger-ui.html**.
Para las rutas protegidas: haz login en `POST /auth/login`, copia el `token` y pégalo en el botón **Authorize**.

## 2. Estructura y capas

| Capa del diseño | Paquetes |
|---|---|
| API | `controller`, `dto`, `config` (seguridad, filtro JWT, CORS, logs) |
| Lógica de negocio | `service/api`, `service/impl`, `entity` |
| Infraestructura | `repository`, `utils` (adaptador de pagos, cortacircuitos, notificadores, generadores PDF/CSV, tareas programadas) |

Regla: el controlador solo llama interfaces `...ServiceAPI`; la lógica y las transacciones están en `...ServiceImpl`.

## 3. Endpoints por historia de usuario

| Épica | Método y ruta | HU | Rol |
|---|---|---|---|
| Salud | `GET /salud` · `GET /salud/metricas` | HU-49 | público · admin |
| E01 | `POST /auth/registro` · `/auth/login` · `/auth/logout` · `/auth/recuperar` · `/auth/restablecer` · `/auth/reactivar` | HU-01 a 04, 06 | público / autenticado |
| E02 | `GET/PUT /usuarios/perfil` (nombre, teléfono, foto) · `PATCH /usuarios/perfil/desactivar` | HU-05, 06 | autenticado |
| E03 | `POST /alojamientos` · `PUT /alojamientos/{id}` (dueño o admin) · `GET /alojamientos/{id}` · `GET /alojamientos/mios` · `PATCH /alojamientos/{id}/estado` · `POST /alojamientos/{id}/imagenes` (URL) · `POST /alojamientos/{id}/imagenes/archivo` (subir foto) · `GET /imagenes/{nombre}` · servicios adicionales | HU-07 a 15 | anfitrión (detalle e imágenes públicos) |
| E04 | `POST /alojamientos/{id}/periodos` · `PUT/DELETE /alojamientos/{id}/periodos/{periodoId}` · `GET /alojamientos/{id}/calendario` | HU-16 a 18 | anfitrión / público |
| E05 | `GET /busqueda` · `GET /busqueda/comparar?ids=1,2,3` (2 o 3) | HU-19 a 22 | público |
| E06 | `POST /cotizaciones` · `GET /tarifas` · `PUT /tarifas/{clave}` | HU-23 a 25 | público / admin |
| E07 | `POST /reservas` (Idempotency-Key) · `POST /reservas/{id}/pagar` · `PATCH /reservas/{id}/aprobar|rechazar` · `GET /reservas/mias` · `GET /reservas/anfitrion?alojamientoId=` · `GET /reservas/{id}` | HU-26 a 31 | huésped / anfitrión |
| E08 | `GET /reservas/{id}/cancelacion/simulacion` · `POST /reservas/{id}/cancelacion` | HU-32 a 35 | huésped |
| E09 | `GET /notificaciones` · `PATCH /notificaciones/{id}/leida` · `PATCH /notificaciones/leidas` | HU-36, 37 | autenticado |
| E10 | `POST /resenas` · `GET /alojamientos/{id}/resenas` · `PATCH /admin/resenas/{id}/ocultar` · `PATCH /admin/resenas/{id}/mostrar` | HU-38 a 41 | huésped / público / admin |
| E11 | `GET /admin/publicaciones/pendientes` · `PATCH /admin/publicaciones/{id}` · `GET/PATCH /admin/usuarios` · `GET /admin/reservas/revision` · `PATCH /admin/reservas/{id}/revision` · `GET /admin/auditoria?usuarioId=&entidad=&accion=&desde=&hasta=` | HU-42 a 45 | admin |
| E12–E13 | `GET /reportes/dashboard` · `GET /reportes/reservas?formato=csv,pdf,excel` · `POST /reportes/exportaciones` · `GET /reportes/exportaciones[/{id}[/archivo]]` | HU-46 a 48 | admin |

Códigos de respuesta: 200/201 éxito · 202 solicitud enviada al anfitrión · 400 datos inválidos · 401 sin sesión ·
402 pago rechazado · 403 rol sin permiso · 404 no existe · 409 conflicto (fechas no disponibles, reserva que ya
no se puede cancelar, periodo con reservas) · 413 archivo muy grande · 422 regla de negocio · 423 cuenta bloqueada ·
503 pasarela o base de datos no disponible. Todas las respuestas de error son `{"mensaje": "..."}`.

## 4. Dónde está cada ADR en el código

| ADR | Dónde |
|---|---|
| ADR-01 Capas | estructura de paquetes; controladores → `ServiceAPI` → repositorios |
| ADR-02 Bloqueo pesimista | `AlojamientoRepository.bloquearPorId` (`@Lock PESSIMISTIC_WRITE`) en `ReservaServiceImpl.crearReserva` |
| ADR-03 Idempotencia 24 h | `ReservaServiceImpl` + tabla `clave_idempotencia`; la clave también se envía a Stripe |
| ADR-04 Pago con timeout y cortacircuitos | `PasarelaPagoAdapter` + `Cortacircuitos` (10 s, 5 fallos, 30 s abierto); el cobro va fuera de la transacción del bloqueo |
| ADR-05 JWT 60 min, BCrypt, bloqueo | `JwtUtil`, `JwtFiltro`, `SecurityConfig`, `AuthServiceImpl` (5 intentos → 15 min), `ListaTokensRevocados` (logout) |
| ADR-06 Auditoría solo inserción | `EventoAuditoriaServiceImpl` (`Propagation.MANDATORY`) + trigger en `schema-extra.sql` |
| ADR-07 Outbox con 3 reintentos | `EventoOutboxServiceImpl` + `DespachadorOutbox` (cada 10 s) |
| ADR-08 Índices y paginación | `@Table(indexes)` en entidades, `PaginaResponse`, búsqueda paginada, reportes |

## 5. Patrones de diseño

| Patrón | Clase | Qué resuelve |
|---|---|---|
| Builder | `entity/Reserva.builder()` | Arma la reserva paso a paso y valida en `build()` |
| Singleton | `utils/MetricasSistema.getInstancia()` | Una sola instancia de métricas para toda la app (observabilidad) |
| Abstract Factory | `utils/FabricaPasarelaPago` → `FabricaPagoStripe`, `FabricaPagoSimulado` | Familia completa de una pasarela (cobro + reembolso) |
| Factory Method | `utils/Notificador.crearCanal()` → `NotificadorInterno`, `NotificadorCorreo` | Cada subclase decide qué canal crea |
| Observador | `utils/DespachadorOutbox` + `ObservadorEvento` | Los notificadores se suscriben a los eventos del outbox |
| Estrategia | `utils/EstrategiaCancelacion` → `CancelacionFlexible`, `CancelacionEstricta` | Cálculo de devolución según la política |
| Estado | `utils/MaquinaEstados` | Transiciones válidas de reserva y publicación |
| Adaptador | `utils/PasarelaPagoAdapter`, `utils/AlmacenamientoImagenAdapter`, `utils/EnviadorCorreo` | El negocio no conoce Stripe, ni dónde se guardan las fotos, ni el servidor de correo |
| Cortacircuitos | `utils/Cortacircuitos` | La app no se cae si la pasarela falla |
| Repositorio | `repository/*` + `GenericServiceImpl.getDao()` | Acceso a datos desacoplado |

## 6. Pagos de prueba

Con `pagos.modo=simulado` (por defecto) el campo `metodoPago` decide el resultado:

| metodoPago | Resultado |
|---|---|
| `pm_card_visa` (o vacío) | aprobado → 201 CONFIRMADA |
| `pm_card_chargeDeclined` | rechazado → 402 PAGO_FALLIDO, fechas liberadas |
| `pm_simular_caida` | la pasarela falla → 503; a los 5 fallos el cortacircuitos se abre |
| `pm_simular_lento` | tarda 12 s → 503 por el tiempo límite de 10 s |

Para usar Stripe real en modo pruebas: en `backend/.env` poner `PAGOS_MODO=stripe` y `STRIPE_SECRET_KEY=sk_test_...`.

## 7. Reglas de cancelación

- **FLEXIBLE**: 100 % si se cancela con 2 o más días de anticipación; 50 % si es con menos.
- **ESTRICTA**: 50 % con 7 o más días; 0 % con menos.
- Si cancela el **administrador** (por ejemplo, una publicación bloqueada) se devuelve el 100 %.
- Si la pasarela falla al devolver, la cancelación sigue y queda `REEMBOLSO_PENDIENTE`; una tarea lo reintenta
  cada 10 minutos con la misma clave de idempotencia (nunca devuelve dos veces).

Si las reglas de negocio del documento de análisis son otras, se cambian en `CancelacionFlexible` y `CancelacionEstricta`.

## 8. Correos (recuperación de clave y notificaciones)

Los correos salen del backend por el outbox (ADR-07): si el envío falla se reintenta hasta 3 veces y la operación
del negocio no se revierte. La clase `EnviadorCorreo` es la única que conoce el servidor SMTP.

| correo.modo | Qué hace |
|---|---|
| `simulado` (por defecto) | No envía nada; deja el aviso en el log. `POST /auth/recuperar` devuelve `tokenRecuperacionSimulado` para probar sin correo. |
| `smtp` | Envía correos reales. `POST /auth/recuperar` ya **no** devuelve el token: llega por correo un enlace a `app.frontend.url/restablecer?token=...` |

Para usar Gmail: activar la verificación en 2 pasos, crear una **contraseña de aplicación** y poner en
`backend/.env` (copia de `backend/.env.example`; git no lo sube), **sin comillas ni espacios**:

```
CORREO_MODO=smtp
CORREO_USUARIO=marketplacealojamientoslr@gmail.com
CORREO_CLAVE=xxxxxxxxxxxxxxxx
```

La aplicación lee ese archivo sola al arrancar: basta `./mvnw spring-boot:run`.
Las pruebas automáticas siempre usan correo y pagos simulados.

El frontend necesita una página `/restablecer` que lea el `token` de la URL y llame a `POST /auth/restablecer`
con `{ "token": "...", "nuevaClave": "..." }`.

## 9. Imágenes de los alojamientos (HU-07)

`AlmacenamientoImagenAdapter` (patrón Adaptador) guarda las fotos en `backend/uploads/imagenes` y devuelve su URL
(`http://localhost:8080/imagenes/<uuid>.jpg`). Solo acepta JPG, PNG y WEBP de hasta 5 MB.

```bash
curl -X POST http://localhost:8080/alojamientos/1/imagenes/archivo \
     -H "Authorization: Bearer <token del anfitrión>" -F "archivo=@foto.jpg"
```

Para usar almacenamiento en la nube solo se cambia esa clase.

## 10. Reportes en segundo plano (HU-47, HU-48)

1. `POST /reportes/exportaciones` con `{"formato": "excel", "desde": "2026-11-01", "hasta": "2026-11-30"}` → **202** y el id.
2. Se genera en un hilo de prioridad mínima (no frena la búsqueda) y llega la notificación "Reporte listo".
3. `GET /reportes/exportaciones/{id}/archivo` descarga el `.xlsx`, `.pdf` o `.csv` (409 si aún no está listo).

Cada solicitud y cada descarga quedan en auditoría con el usuario, la fecha, el tipo y los filtros.

## 11. Logs y salud (HU-49)

- Cada solicitud lleva el encabezado `X-Correlation-Id` (si no llega, se genera).
- La consola muestra `[correlacionId] [usuario=...]` en cada línea y `logs/marketplace.log` guarda los mismos logs en
  **JSON** (nivel, fecha, `correlacionId`, `usuarioId`, `operacion`). No se escriben claves, tokens ni tarjetas.
- `GET /salud` revisa la base de datos (3 s máximo) y el cortacircuitos de pagos: `ok`, `degradado` o `caido` (503).

## 12. Más documentación

- [Despliegue, variables de entorno, CI y calidad](DESPLIEGUE.md)
- [Matriz de trazabilidad HU → código → prueba](MATRIZ_TRAZABILIDAD.md)
- Pruebas de carga ESC-01 y ESC-02: `docs/jmeter/marketplace_carga.jmx`
- Documentación interactiva: http://localhost:8080/swagger-ui.html

