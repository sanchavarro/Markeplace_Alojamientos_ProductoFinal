# Matriz de trazabilidad (MKT-107)

De cada historia de usuario (Jira MKT-48 a MKT-96) al endpoint, las clases que la implementan y la prueba que la verifica.
Las pruebas JUnit están en `backend/src/test`; las de Postman en `docs/Marketplace_LR.postman_collection.json` (número de carpeta).

| HU | Jira | Historia | MoSCoW | Endpoint | Implementación | Prueba |
|---|---|---|---|---|---|---|
| HU-01 | MKT-48 | Registro de usuario con rol | Must | `POST /auth/registro` | AuthServiceImpl.registrar, RegistroRequest, BCrypt (SecurityConfig) | Postman 1 · SeguridadApiTest.elRegistroValidaLosDatosYNoPermiteAdministrador |
| HU-02 | MKT-49 | Iniciar y cerrar sesión | Must | `POST /auth/login · POST /auth/logout` | AuthServiceImpl.login/logout, JwtUtil, ListaTokensRevocados | Postman 1 y 12 · ReservaServiceImplTest.debeBloquearLaCuentaTrasCincoIntentosFallidos |
| HU-03 | MKT-50 | Proteger rutas según el rol | Must | `Todas las rutas privadas` | SecurityConfig, JwtFiltro, AlojamientoServiceImpl.obtenerPropio | Postman 3 y 11 (401/403) · SeguridadApiTest |
| HU-04 | MKT-51 | Recuperación de contraseña | Should | `POST /auth/recuperar · POST /auth/restablecer` | AuthServiceImpl.solicitarRecuperacion/restablecerClave, EnviadorCorreo, NotificadorCorreo | Postman 1 |
| HU-05 | MKT-52 | Consultar y actualizar mi perfil | Should | `GET/PUT /usuarios/perfil` | UsuarioServiceImpl.perfil/actualizarPerfil | Postman 1 |
| HU-06 | MKT-53 | Activar o desactivar mi cuenta | Should | `PATCH /usuarios/perfil/desactivar · POST /auth/reactivar` | UsuarioServiceImpl.desactivarCuenta, AuthServiceImpl.reactivar | ReglasNegocioTest.noSeDesactivaLaCuentaConReservasPendientes |
| HU-07 | MKT-54 | Crear publicación de alojamiento | Must | `POST /alojamientos · POST /alojamientos/{id}/imagenes[/archivo]` | AlojamientoServiceImpl.crear/subirImagen, AlmacenamientoImagenAdapter | Postman 3 · AlmacenamientoImagenAdapterTest |
| HU-08 | MKT-55 | Editar publicación | Must | `PUT /alojamientos/{id}` | AlojamientoServiceImpl.editar (dueño o administrador) | Postman 4 (administrador edita) |
| HU-09 | MKT-56 | Gestionar estados de la publicación | Must | `PATCH /alojamientos/{id}/estado · PATCH /admin/publicaciones/{id}` | MaquinaEstados (PAT-06), AlojamientoServiceImpl.cambiarEstado | MaquinaEstadosTest · Postman 3 y 4 |
| HU-10 | MKT-57 | Activar y pausar mi publicación | Must | `PATCH /alojamientos/{id}/estado` | AlojamientoServiceImpl.cambiarEstadoPorAnfitrion | MaquinaEstadosTest |
| HU-11 | MKT-58 | Validar información mínima | Must | `PATCH /alojamientos/{id}/estado (ENVIAR_REVISION)` | AlojamientoServiceImpl.validarInformacionMinima | Postman 3 (422) |
| HU-12 | MKT-59 | Validar capacidad y precio | Must | `POST/PUT /alojamientos` | AlojamientoServiceImpl.aplicarDatos | Postman 3 (422) |
| HU-13 | MKT-60 | Limpieza, servicios adicionales y política | Must | `POST/DELETE /alojamientos/{id}/servicios-adicionales` | AlojamientoServiceImpl.agregarServicioAdicional, politicaId | Postman 3 |
| HU-14 | MKT-61 | Reserva inmediata o con aprobación | Must | `POST /reservas · PATCH /reservas/{id}/aprobar · /rechazar` | ReservaServiceImpl.reservar/aprobar/rechazar | Postman 7 |
| HU-15 | MKT-62 | Detalle de una publicación | Must | `GET /alojamientos/{id}` | AlojamientoServiceImpl.detalle/aDTO | Postman 5 |
| HU-16 | MKT-63 | Administrar periodos disponibles | Must | `POST/PUT/DELETE /alojamientos/{id}/periodos` | DisponibilidadServiceImpl.crear/editar/eliminarPeriodo | Postman 3 · ReglasNegocioTest.noSeBorraUnPeriodoConReservas |
| HU-17 | MKT-64 | Calendario de disponibilidad | Must | `GET /alojamientos/{id}/calendario` | DisponibilidadServiceImpl.calendario | Postman 5 |
| HU-18 | MKT-65 | Bloquear periodos reservados | Must | `POST /reservas` | DisponibilidadServiceImpl.bloquearPorReserva/liberarPorReserva | ReservaServiceImplTest.debeLiberarLasFechasCuandoElPagoEsRechazado |
| HU-19 | MKT-66 | Buscar por ciudad, fechas y capacidad | Must | `GET /busqueda` | BusquedaServiceImpl.buscar, AlojamientoRepository.buscar, índices (ADR-08) | Postman 5 · JMeter ESC-01 |
| HU-20 | MKT-67 | Filtrar por tipo, precio y servicios | Must | `GET /busqueda` | BusquedaServiceImpl.buscar | Postman 5 |
| HU-21 | MKT-68 | Solo publicaciones activas y disponibles | Must | `GET /busqueda` | AlojamientoRepository.buscar (ACTIVA, anfitrión activo, sin cruces) | Postman 5 |
| HU-22 | MKT-69 | Comparar alojamientos | Could | `GET /busqueda/comparar?ids=` | BusquedaServiceImpl.comparar (2 a 3) | Postman 5 (422) |
| HU-23 | MKT-70 | Calcular la cotización | Must | `POST /cotizaciones` | CotizacionServiceImpl.calcular | Postman 6 · JMeter ESC-02 |
| HU-24 | MKT-71 | Configurar porcentajes de tarifas | Must | `GET /tarifas · PUT /tarifas/{clave}` | ConfiguracionTarifaServiceImpl.actualizar | Postman 11 |
| HU-25 | MKT-72 | Salida posterior a la llegada | Must | `POST /cotizaciones · POST /reservas` | Utilidad.validarFechas | Postman 6 (422) |
| HU-26 | MKT-73 | Crear reserva inmediata | Must | `POST /reservas` | ReservaServiceImpl.reservar, Reserva.builder() (Builder), PasarelaPagoAdapter | Postman 7 |
| HU-27 | MKT-74 | Revalidar disponibilidad (sin superposiciones) | Must | `POST /reservas` | AlojamientoRepository.bloquearPorId (@Lock PESSIMISTIC_WRITE, ADR-02) | ReservaServiceImplTest (10 hilos × 100 repeticiones) · Postman 7 (409) |
| HU-28 | MKT-75 | Evitar reservas y cobros duplicados | Must | `POST /reservas (Idempotency-Key)` | ClaveIdempotenciaRepository, ReservaServiceImpl (ADR-03) | ReservaServiceImplTest.debeCobrarUnaSolaVezConLaMismaIdempotencyKey · Postman 7 |
| HU-29 | MKT-76 | Historial de mis reservas | Should | `GET /reservas/mias` | ReservaServiceImpl.historialHuesped | Postman 7 |
| HU-30 | MKT-77 | Reservas de mis alojamientos | Should | `GET /reservas/anfitrion?alojamientoId=` | ReservaServiceImpl.reservasAnfitrion | Postman 7 |
| HU-31 | MKT-78 | Pago con Stripe (modo pruebas) | Must | `POST /reservas · POST /reservas/{id}/pagar` | PasarelaPagoAdapter (PAT-01), FabricaPagoStripe/Simulado (Abstract Factory), Cortacircuitos (PAT-05), TareasProgramadas | CortacircuitosTest · Postman 7 (402/503) |
| HU-32 | MKT-79 | Cancelar una reserva | Must | `POST /reservas/{id}/cancelacion` | CancelacionServiceImpl.cancelar | CancelacionServiceImplTest.soloElDuenoCancelaYNoDosVeces · Postman 8 (409) |
| HU-33 | MKT-80 | Política y cálculo de la devolución | Must | `GET /reservas/{id}/cancelacion/simulacion` | EstrategiaCancelacion, CancelacionFlexible/Estricta (PAT-02), reintento de REEMBOLSO_PENDIENTE | EstrategiaCancelacionTest · CancelacionServiceImplTest |
| HU-34 | MKT-81 | Registrar los datos de la cancelación | Must | `POST /reservas/{id}/cancelacion` | Cancelacion (tabla), EventoAuditoriaServiceImpl | CancelacionServiceImplTest.debeCancelarDevolverTodoYLiberarLasFechas |
| HU-35 | MKT-82 | Liberar el periodo cancelado | Must | `POST /reservas/{id}/cancelacion` | DisponibilidadServiceImpl.liberarPorReserva | CancelacionServiceImplTest.debeCancelarDevolverTodoYLiberarLasFechas |
| HU-36 | MKT-83 | Registrar notificaciones | Should | (outbox) | EventoOutboxServiceImpl, DespachadorOutbox (Observador), NotificadorInterno/Correo (Factory Method) | ReglasNegocioTest.elOutboxCreaLasNotificaciones · Postman 9 |
| HU-37 | MKT-84 | Consultar y marcar notificaciones | Should | `GET /notificaciones · PATCH /notificaciones/{id}/leida · PATCH /notificaciones/leidas` | NotificacionServiceImpl | Postman 9 |
| HU-38 | MKT-85 | Calificar y reseñar | Should | `POST /resenas` | ResenaServiceImpl.crear | Postman 10 (422) |
| HU-39 | MKT-86 | Relación de la reseña | Should | `POST /resenas` | Resena (reserva, huésped, alojamiento, fecha) | Postman 10 |
| HU-40 | MKT-87 | Reseñas y promedio | Should | `GET /alojamientos/{id}/resenas` | ResenaServiceImpl.listar | Postman 10 |
| HU-41 | MKT-88 | Ocultar reseñas | Should | `PATCH /admin/resenas/{id}/ocultar · /mostrar` | ResenaServiceImpl.cambiarVisibilidad | Postman (manual) |
| HU-42 | MKT-89 | Bloquear publicación | Must | `PATCH /admin/publicaciones/{id} (BLOQUEAR) · GET/PATCH /admin/reservas/revision` | AlojamientoServiceImpl.marcarReservasParaRevision, AdministracionServiceImpl | ReglasNegocioTest.bloquearPublicacionMarcaLasReservasParaRevision · Postman 11 |
| HU-43 | MKT-90 | Aprobar o pausar publicaciones | Must | `GET /admin/publicaciones/pendientes · PATCH /admin/publicaciones/{id}` | AdministracionServiceImpl.gestionarPublicacion | Postman 4 |
| HU-44 | MKT-91 | Restringir o reactivar usuarios | Should | `PATCH /admin/usuarios/{id}` | UsuarioServiceImpl.cambiarEstadoPorAdministrador, JwtFiltro (estado en cada solicitud) | Postman 11 |
| HU-45 | MKT-92 | Consultar auditoría | Should | `GET /admin/auditoria` | EventoAuditoriaServiceImpl.consultar, trigger solo inserción (ADR-06) | ReglasNegocioTest.laAuditoriaNoSePuedeEditarNiBorrar · Postman 11 |
| HU-46 | MKT-93 | Dashboard | Should | `GET /reportes/dashboard` | ReporteServiceImpl.dashboard | Postman 11 |
| HU-47 | MKT-94 | Exportar reportes PDF/Excel | Should | `GET /reportes/reservas · POST/GET /reportes/exportaciones` | ReporteServiceImpl (hilo de baja prioridad), GeneradorPdf, GeneradorExcel, GeneradorCsv | ReglasNegocioTest.elReporteSeGeneraEnSegundoPlano · GeneradorExcelTest · Postman 11 |
| HU-48 | MKT-95 | Registrar quién exporta | Should | `GET /reportes/reservas · POST /reportes/exportaciones` | EventoAuditoriaServiceImpl (EXPORTAR_REPORTE, SOLICITAR_EXPORTACION) | Postman 11 |
| HU-49 | MKT-96 | Logs estructurados | Must | `Todas · GET /salud · GET /salud/metricas` | FiltroCorrelacion (MDC), logs JSON, SaludServiceImpl, MetricasSistema (Singleton) | SeguridadApiTest.saludEsPublicaYReportaLaBaseDeDatos · Postman 0 |

## ADR y ASR

| ADR | Decisión | Dónde | Prueba |
|---|---|---|---|
| ADR-01 | Capas | controller → service/api → service/impl → repository | Revisión (CodeRabbit) |
| ADR-02 | Bloqueo pesimista | `AlojamientoRepository.bloquearPorId` | ReservaServiceImplTest (×100) |
| ADR-03 | Idempotencia 24 h | `clave_idempotencia`, `ReservaServiceImpl` | ReservaServiceImplTest |
| ADR-04 | Timeout 10 s + cortacircuitos | `PasarelaPagoAdapter`, `Cortacircuitos` | CortacircuitosTest · Postman 7 (503) |
| ADR-05 | JWT 60 min, BCrypt, bloqueo 15 min | `JwtUtil`, `JwtFiltro`, `AuthServiceImpl` | ReservaServiceImplTest · SeguridadApiTest |
| ADR-06 | Auditoría solo inserción | `EventoAuditoriaServiceImpl`, trigger | ReglasNegocioTest |
| ADR-07 | Outbox con 3 reintentos | `EventoOutboxServiceImpl`, `DespachadorOutbox` | ReglasNegocioTest |
| ADR-08 | Índices, paginación y tareas en segundo plano | `@Table(indexes)`, `PaginaResponse`, `ReporteServiceImpl` | JMeter ESC-01/02 · ReglasNegocioTest |

## Diferencias con el texto de las historias

- **HU-33** habla de una interfaz `PoliticaCancelacion`; en el código se llama `EstrategiaCancelacion` porque `PoliticaCancelacion` ya es la entidad de la tabla `politica_cancelacion`.
- **HU-04** pide un enlace simulado: en modo `simulado` el token sale en la respuesta; en modo `smtp` llega por correo real.
- **HU-47** se ofrece de dos formas: descarga inmediata (`GET /reportes/reservas`) y en segundo plano con aviso (`POST /reportes/exportaciones`).
