# Estancia · Frontend del Marketplace de Reserva y Gestión de Alojamientos

Frontend en **React 18 + Vite 5 + Tailwind CSS 3** para la plataforma de reserva y gestión de alojamientos desarrollada como proyecto de grado de Ingeniería de Sistemas. Consume la API REST del backend Spring Boot (`Markeplace_Alojamientos_ProductoFinal`) y cubre las 49 historias de usuario del proyecto **MKT** en Jira (HU-01 a HU-49, épicas E01 a E14).

- Diseño "Apple Hospitality": minimalismo, mucho espacio en blanco, bordes `rounded-2xl/3xl`, fondos `#FFFFFF`/`#F5F5F7`, texto `#1D1D1F`, glassmorphism (`backdrop-blur-md bg-white/75`) en la barra de navegación, modales, barra de comparación y tarjeta de reserva.
- Fotografía protagonista estilo Airbnb: mini-carrusel en cada tarjeta, mosaico Bento de 5 fotos en el detalle y lightbox a pantalla completa.

---

## Novedades de la versión 1.1

| Cambio | Dónde |
|---|---|
| Página de inicio didáctica: destacados centrados y secciones "Descubre nuevos espacios", tipos de alojamiento, cómo funciona, beneficios, banner para anfitriones y preguntas frecuentes | `pages/public/HomePage.jsx`, `components/home/Secciones.jsx` |
| Catálogo completo con filtros movido a `/explorar` (grilla centrada) | `pages/public/ExplorarPage.jsx` |
| Nuevas páginas: Destinos, Cómo funciona, Hazte anfitrión y Centro de ayuda | `pages/public/InfoPages.jsx` |
| Barra superior con "Inicio" bajo el logo y más opciones (Explorar, Destinos, Cómo funciona, Hazte anfitrión, Ayuda) + acceso directo del rol | `components/layout/Navbar.jsx` |
| Footer con "¿Necesitas algo? Comunícate con Marketplace", correo, teléfono, columnas de enlaces y redes sociales | `components/layout/Layouts.jsx`, variables `VITE_CORREO_CONTACTO`, `VITE_URL_*` |
| Imagen redondeada en inicio de sesión y registro | `AuthPages.jsx › AuthShell` |
| Registro con captcha y confirmación del correo por código de 6 dígitos | `AuthPages.jsx › Captcha, VerificarCorreoPage`; backend `/auth/captcha`, `/auth/verificar-correo`, `/auth/reenviar-verificacion` |
| Pasarela de pago: checkout en dos pasos (revisar el viaje → pagar con tarjeta) con validación Luhn, franquicia, vencimiento y CVC | `components/reservas/Pasarela.jsx` |

### Tarjetas de prueba de la pasarela

| Número | Resultado |
|---|---|
| 4242 4242 4242 4242 | Aprobada (Visa) |
| 5555 5555 5555 4444 | Aprobada (Mastercard) |
| 4000 0000 0000 0002 | Rechazada |
| 4000 0000 0000 9995 | Rechazada por fondos insuficientes |

Cualquier fecha futura y cualquier CVC. Los datos de la tarjeta no salen del navegador: se convierten en un método de pago de prueba de Stripe (`pm_card_visa`, `pm_card_chargeDeclined`…), que es lo único que recibe el backend.

### Cuentas de demostración (creadas por el backend)

| Rol | Correo | Clave |
|---|---|---|
| Administrador | admin@marketplace.com | Admin2026* |
| Anfitrión (dueño de los 18 alojamientos de ejemplo) | anfitrion.demo@marketplace.com | Demo2026* |
| Huésped | huesped.demo@marketplace.com | Demo2026* |

---

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| Node.js | 18.18 o superior (recomendado 20 LTS) |
| npm | 9 o superior |
| Backend | Spring Boot en `http://localhost:8080` con PostgreSQL (ver repositorio del backend) |

## 2. Instalación y ejecución

```bash
# 1. Instalar dependencias
npm install

# 2. Configurar el entorno
cp .env.example .env        # en Windows: copy .env.example .env

# 3. Levantar el backend (en su carpeta): ./mvnw spring-boot:run

# 4. Levantar el frontend
npm run dev                 # http://localhost:5173
```

Otros scripts:

```bash
npm run build     # compilación de producción en dist/
npm run preview   # sirve dist/ en http://localhost:5173
npm run lint      # ESLint (React + Hooks)
```

> **Importante:** abre la aplicación en `http://localhost:5173`, no en `http://127.0.0.1:5173`. El backend solo acepta el origen exacto `http://localhost:5173` en CORS (ver sección 6).

### Variables de entorno (`.env`)

| Variable | Valor por defecto | Uso |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | URL base del backend (sin barra final ni prefijo `/api`). |
| `VITE_NOTIFICACIONES_INTERVALO_MS` | `30000` | Refresco del contador de la campana. |
| `VITE_EXPORTACIONES_INTERVALO_MS` | `4000` | Sondeo del estado de exportaciones. |
| `VITE_CORREO_CONTACTO` | `contacto@marketplace.com` | Correo de contacto del footer y del centro de ayuda. |
| `VITE_TELEFONO_CONTACTO` | `+57 601 555 0100` | Teléfono de contacto. |
| `VITE_URL_INSTAGRAM`, `VITE_URL_FACEBOOK`, `VITE_URL_X`, `VITE_URL_TIKTOK`, `VITE_URL_YOUTUBE` | Página principal de cada red | Perfiles de la marca en el footer (vacío = se oculta el ícono). |

### Usuarios de prueba

El backend crea al iniciar el administrador `admin@marketplace.com` / `Admin2026*` (`application.properties`). Los huéspedes y anfitriones se registran desde `/registro`.

---

## 3. Arquitectura

```
src/
├── api/                    Capa de acceso a datos (un módulo por controlador del backend)
│   ├── client.js           Axios: baseURL, interceptores (Bearer, 401/403), normalización de errores,
│   │                       Idempotency-Key, serialización de listas (?servicios=1&servicios=2), descargas Blob
│   ├── auth.api.js         /auth
│   ├── usuarios.api.js     /usuarios
│   ├── catalogo.api.js     /busqueda, /catalogo, /cotizaciones, /tarifas
│   ├── alojamientos.api.js /alojamientos (público y anfitrión, imágenes, periodos, servicios)
│   ├── reservas.api.js     /reservas y /reservas/{id}/cancelacion
│   ├── social.api.js       /resenas, /notificaciones
│   └── admin.api.js        /admin, /reportes, /tarifas/{clave}, /salud
├── context/                Estado global: AuthContext, ToastContext, NotificationsContext, CompareContext
├── hooks/                  useAsync, useIdempotencia, useDebounce, useCatalogos, useMiniaturas
├── routes/RoleGuard.jsx    Protección por rol y rutas solo para invitados
├── components/
│   ├── ui/                 Sistema de diseño: Button, Input, Select, Textarea, Modal, Confirmar,
│   │                       Badge, Skeleton, EmptyState, Estrellas, Paginacion, SafeImage…
│   ├── layout/             Navbar glass, campana de notificaciones, layouts e indicador de salud
│   ├── listings/           ListingCard + carrusel, SearchBar, FiltersModal, CompareBar,
│   │                       Gallery (Bento + Lightbox), BookingCard, AvailabilityCalendar, ReviewsSection
│   └── reservas/           Resultado de reserva, cancelación, pago y reseña
├── pages/
│   ├── public/             Catálogo, detalle, comparar, login, registro, recuperar, restablecer, 403/404
│   ├── shared/             Perfil, notificaciones, detalle de reserva
│   ├── guest/              Mis viajes
│   ├── host/               Mis alojamientos, formulario, gestión (fotos, servicios, periodos), reservas
│   └── admin/              Dashboard, moderación, usuarios, reseñas, revisión, auditoría, reportes, tarifas, salud
└── utils/                  env, constantes de dominio, formato (COP, fechas sin desfase), validadores, imágenes, sesión
```

**Principios aplicados**

- **Capas:** las vistas nunca llaman a Axios directamente; usan los módulos de `src/api`, que reproducen exactamente los nombres de campo de los DTO del backend.
- **Fuente de verdad en el servidor:** la cotización, la disponibilidad y las reglas de negocio se calculan en el backend; el cliente valida antes de enviar (espejo de `jakarta.validation`) y muestra el error del servidor por campo cuando llega `{"mensaje","errores"}`.
- **Carga diferida por rol:** cada página se carga con `React.lazy`, así un huésped no descarga el panel de administración.
- **Sin datos de ejemplo en el código:** todas las vistas consumen la API. La única excepción deliberada son las fotos de respaldo (`utils/images.js`) para alojamientos sin imágenes.

### Rutas

| Ruta | Acceso | Vista |
|---|---|---|
| `/` | Público | Catálogo y búsqueda |
| `/alojamientos/:id` | Público | Detalle, calendario, reseñas y reserva |
| `/comparar?ids=1,2,3` | Público | Comparador |
| `/login`, `/registro`, `/recuperar` | Solo invitados | Autenticación |
| `/restablecer?token=` | Público | Nueva clave (enlace del correo) |
| `/perfil`, `/notificaciones`, `/reservas/:id` | Con sesión | Compartidas |
| `/mis-reservas` | Huésped | Historial de viajes |
| `/anfitrion/**` | Anfitrión | Panel del anfitrión |
| `/admin/**` | Administrador | Panel de administración |

---

## 4. Seguridad

| Aspecto | Implementación |
|---|---|
| Inicio de sesión | `POST /auth/login` → se guarda el token y **de inmediato** se pide `GET /usuarios/perfil`; el rol y el estado salen de ahí. Si el perfil falla, la sesión se descarta por completo. |
| Almacenamiento del JWT | `sessionStorage` + memoria, con la fecha de expiración (`expiraEnSegundos`). Se descarta localmente antes de vencer y un temporizador cierra la sesión a los 60 minutos. No se guarda ningún otro dato sensible. |
| Cierre de sesión | `POST /auth/logout` revoca el token en el servidor **antes** de limpiar el estado local (se limpia siempre, aunque el token ya hubiera vencido). |
| Interceptores Axios | Inyectan `Authorization: Bearer <token>`. Un 401 con token (vencido, revocado o usuario restringido) limpia la sesión, avisa al usuario y reintenta una vez sin token para que las rutas públicas sigan funcionando. 403 se muestra como falta de permiso. |
| Idempotencia | `POST /reservas` y `POST /reservas/{id}/pagar` envían `Idempotency-Key` generado con `crypto.randomUUID()` (con respaldo RFC 4122). La clave se reutiliza si el resultado es incierto (red, timeout, 5xx) y se renueva tras una respuesta definitiva. El botón se bloquea durante el envío. |
| Roles | `RoleGuard` separa vistas públicas, de huésped, de anfitrión y de administrador; el backend vuelve a validar cada solicitud. |
| Cuenta desactivada | Un 423 "cuenta desactivada" en el login abre una pantalla para reactivarla con `POST /auth/reactivar`. |
| XSS | React escapa todo el contenido; no se usa `dangerouslySetInnerHTML`. |

---

## 5. Relación con Jira

La matriz completa HU → endpoint → vista → criterio de aceptación está en [`docs/MATRIZ_TRAZABILIDAD_FRONTEND.md`](docs/MATRIZ_TRAZABILIDAD_FRONTEND.md). Resumen por épica:

| Épica | Historias | Módulos del frontend |
|---|---|---|
| E01 Registro e inicio de sesión | HU-01 a HU-04 | `AuthContext`, `AuthPages`, `RoleGuard`, `api/client.js` |
| E02 Perfil | HU-05, HU-06 | `PerfilPage`, reactivación en `LoginPage` |
| E03 Alojamientos y publicaciones | HU-07 a HU-15 | `AlojamientoFormPage`, `GestionAlojamientoPage`, `AlojamientoDetailPage`, `ModeracionPage` |
| E04 Disponibilidad | HU-16 a HU-18 | `AvailabilityCalendar`, pestaña Disponibilidad |
| E05 Búsqueda | HU-19 a HU-22 | `HomePage`, `SearchBar`, `FiltersModal`, `ComparePage` |
| E06 Cotización | HU-23 a HU-25 | `BookingCard`, `TarifasPage`, validadores de fechas |
| E07 Reserva | HU-26 a HU-31 | `BookingCard`, `PagoModal`, `MisReservasPage`, `HostReservasPage` |
| E08 Cancelación | HU-32 a HU-35 | `CancelacionModal` |
| E09 Notificaciones | HU-36, HU-37 | `NotificationsContext`, `NotificationBell`, `NotificacionesPage` |
| E10 Reseñas | HU-38 a HU-41 | `ResenaModal`, `ReviewsSection`, `ResenasAdminPage` |
| E11 Administración | HU-42 a HU-45 | `ModeracionPage`, `UsuariosPage`, `ReservasRevisionPage`, `AuditoriaPage` |
| E12 Dashboard | HU-46 | `AdminDashboardPage` |
| E13 Reportes | HU-47, HU-48 | `ReportesPage` |
| E14 Trazabilidad y observabilidad | HU-49 | `IndicadorSalud`, `SaludPage` |

---

## 6. Hallazgos técnicos del backend (auditoría)

Puntos verificados en el código del backend que condicionan el frontend:

1. **Puerto y prefijo:** `server.port=8080`; las rutas cuelgan de la raíz (`/auth`, `/busqueda`…), **no** hay prefijo `/api`.
2. **CORS:** `SecurityConfig` acepta solo `app.frontend.url=http://localhost:5173` (y los `@CrossOrigin` de cada controlador fijan el mismo origen). Por eso Vite usa `strictPort` en 5173 y hay que abrir `localhost`, no `127.0.0.1`. Expone `X-Correlation-Id` y `Content-Disposition` (este último se usa para el nombre de los reportes).
3. **JWT:** dura 60 minutos y no hay *refresh token*; al vencer se debe iniciar sesión de nuevo.
4. **Token inválido en rutas públicas:** `JwtFiltro` responde 401 a cualquier solicitud con un token vencido o revocado, aunque la ruta sea pública. El interceptor lo resuelve reintentando sin token.
5. **Cuenta desactivada:** el login responde **423** con el mensaje "Tu cuenta está desactivada…", no un código propio; el frontend lo detecta por estado + mensaje.
6. **Idempotency-Key es obligatoria** en reservar y pagar (sin ella el backend responde 400) y debe ser un UUID.
7. **Códigos de reservar/pagar:** 201 confirmada, 202 enviada al anfitrión, 402 pago rechazado y 503 pasarela caída; en 402/503 el cuerpo sigue siendo un `ReservaResponse`.
8. **Imágenes:** el adaptador de almacenamiento devuelve URLs absolutas (`http://localhost:8080/imagenes/{nombre}`); el frontend también resuelve rutas relativas y nombres sueltos contra `VITE_API_BASE_URL`.
9. **`ReservaResponse` no trae la foto del alojamiento:** las miniaturas se obtienen con `GET /alojamientos/{id}` (en caché por pestaña).
10. **No existe un endpoint para listar reseñas ocultas:** el panel las reconstruye desde la auditoría (`OCULTAR_RESENA` / `MOSTRAR_RESENA`).
11. **`Resena` no incluye el nombre del huésped**, por eso las reseñas se muestran como "Huésped verificado".
12. **Cancelación:** el backend permite cancelar reservas `CONFIRMADA`, `PENDIENTE_PAGO` y `EN_REVISION` antes de la llegada, mientras que el criterio de HU-32 en Jira menciona solo `CONFIRMADA`. El frontend sigue al backend; conviene alinear el criterio en Jira.
13. **Estado en Jira:** HU-22 y HU-34 a HU-48 figuran "Por hacer" en Jira aunque sus endpoints ya existen en el backend. El frontend las implementa todas.
14. **Recuperación de clave:** el enlace del correo es `app.frontend.url + /restablecer?token=…`; con `correo.modo=simulado` el token llega en `tokenRecuperacionSimulado`.

---

## 7. Diseño

- **Tipografía:** pila del sistema (SF Pro en Apple, Inter/Helvetica en el resto), interletrado negativo en títulos.
- **Color:** blanco y `#F5F5F7` como superficies, `#1D1D1F` para el texto, azul `#0071E3` para enlaces y un acento coral para las acciones de reserva.
- **Micro-interacciones:** transiciones de 200–700 ms, zoom sutil en las fotos, *skeletons* con la misma estructura de las tarjetas, toasts glass.
- **Responsive:** carrusel táctil en móvil, Bento grid en escritorio, menú móvil y tablas con desplazamiento horizontal.
- **Accesibilidad:** etiquetas en todos los campos, `aria-invalid`/`role="alert"` en errores, foco gestionado en modales, navegación con teclado en el lightbox y tablas alternativas para las gráficas.

---

## 8. Créditos

Proyecto de grado · Ingeniería de Sistemas · Universidad El Bosque. Equipo: Carlos Luna, Andrés Jarro y Santiago Chavarro (responsables registrados en Jira).
