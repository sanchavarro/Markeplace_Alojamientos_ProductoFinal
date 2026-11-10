# Marketplace de Alojamientos — Inversiones LR

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sanchavarro_Marketplace-Alojamientos-Producto&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sanchavarro_Marketplace-Alojamientos-Producto)

Proyecto de Ingeniería de Software · Universidad El Bosque.

Plataforma web donde el **anfitrión** publica alojamientos, el **huésped** busca, cotiza y reserva pagando en línea,
y el **administrador** modera publicaciones, gestiona usuarios y consulta reportes.

## Equipo

| Integrante | Rol |
|---|---|
| Santiago Chavarro | Gerente de proyecto / Scrum Master |
| Andrés Jarro | Analista |
| Carlos Luna | QA |

## Tecnologías

- Backend: Java 21, Spring Boot 3.5, Spring Data JPA, Spring Security + JWT
- Base de datos: PostgreSQL 17
- Pagos: modo simulado por defecto, Stripe en modo pruebas
- CI: GitHub Actions

## Cómo ejecutarlo

```bash
psql postgres -f docs/base_de_datos.sql   # crea las bases y el usuario
cd backend
./mvnw spring-boot:run                    # API en http://localhost:8080
./mvnw test                               # pruebas
```

Para correos reales o Stripe en modo pruebas, copia `backend/.env.example` como `backend/.env` y llénalo.

| Documento | Contenido |
|---|---|
| [docs/GUIA_BACKEND.md](docs/GUIA_BACKEND.md) | Endpoints por historia, ADR, patrones, pagos y cancelación |
| [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) | Variables de entorno, despliegue, CI, SonarCloud, CodeRabbit y JMeter |
| [docs/MATRIZ_TRAZABILIDAD.md](docs/MATRIZ_TRAZABILIDAD.md) | De cada historia de usuario al código y a su prueba |
| `docs/Marketplace_LR.postman_collection.json` | Colección de Postman (Run collection) |
| http://localhost:8080/swagger-ui.html | Documentación interactiva de la API |


## Cambios de la versión 1.1 (registro seguro y catálogo de demostración)

- **Captcha propio** en el registro: `GET /auth/captcha` devuelve `{ captchaId, imagen (SVG en base64), expiraEnSegundos, habilitado }`. `POST /auth/registro` recibe además `captchaId` y `captchaRespuesta`. Cada captcha es de un solo uso y vence en 5 minutos (`utils/ServicioCaptcha.java`).
- **Confirmación del correo**: al registrarse se envía (por el outbox) un código de 6 dígitos que vence en 15 minutos; solo se guarda su hash BCrypt. Hasta confirmarlo, el login responde **403** `{ "codigo": "CORREO_NO_VERIFICADO" }`.
  - `POST /auth/verificar-correo { correo, codigo }` → confirma y devuelve la sesión (`LoginResponse`). Máximo 5 intentos por código.
  - `POST /auth/reenviar-verificacion { correo }` → código nuevo (uno por minuto).
  - Con `correo.modo=simulado` el código también llega en la respuesta (`codigoVerificacionSimulado`) para poder probar sin correo real.
  - Las cuentas creadas antes de este cambio (columna `correo_verificado` vacía) se consideran verificadas.
- **Catálogo de demostración** (`config/DatosDemo.java`): 18 alojamientos ACTIVOS en 16 destinos de Colombia, cada uno con fotos distintas, servicios, extras y disponibilidad por 12 meses. Se crea una sola vez junto con `anfitrion.demo@marketplace.com` y `huesped.demo@marketplace.com` (clave `Demo2026*`).
- Nuevas propiedades (todas con variable de entorno):

| Propiedad | Por defecto | Variable |
|---|---|---|
| `seguridad.captcha.habilitado` | `true` | `CAPTCHA_HABILITADO` |
| `seguridad.verificacion.correo.habilitada` | `true` | `VERIFICACION_CORREO` |
| `seguridad.verificacion.correo.minutos` | `15` | — |
| `datos.demo` | `true` | `DATOS_DEMO` |

- Columnas nuevas en `usuario` (las agrega `ddl-auto=update`): `correo_verificado`, `codigo_verificacion_hash`, `codigo_verificacion_expira`, `codigo_verificacion_enviado`, `intentos_verificacion`.
- En `application-test.properties` el captcha, la verificación y el catálogo demo están desactivados para que las pruebas existentes sigan pasando.
