# Despliegue del backend (MKT-105)

Guía para dejar la API corriendo en un servidor (o en otro computador) con PostgreSQL, correo real y,
si se quiere, Stripe en modo pruebas.

## 1. Requisitos

| Componente | Versión |
|---|---|
| Java (JDK) | 21 |
| PostgreSQL | 17 (sirve 15 o superior) |
| Maven | no hace falta: el proyecto trae `./mvnw` |

## 2. Base de datos

```bash
psql postgres -f docs/base_de_datos.sql
```

Crea el usuario `marketplace` y las bases `marketplace_db` y `marketplace_test`.
En un servidor real cambia la clave del usuario y pásala con `SPRING_DATASOURCE_PASSWORD`.

Las tablas, índices, llaves foráneas, CHECK y el trigger de auditoría se crean solos al arrancar
(`ddl-auto=update` + `schema-extra.sql`). Los datos iniciales (políticas, servicios, tarifas y el
administrador) también.

## 3. Variables de entorno

Ningún secreto va en el código. En local se escriben en `backend/.env` (copia de `.env.example`, git lo ignora);
en un servidor se definen como variables de entorno.

| Variable | Para qué | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Base de datos | `jdbc:postgresql://db:5432/marketplace_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `marketplace` |
| `SPRING_DATASOURCE_PASSWORD` | Clave de la base | — |
| `JWT_SECRET` | Firma de los tokens (Base64, 64 bytes o más) | `openssl rand -base64 64` |
| `APP_FRONTEND_URL` | URL del frontend (CORS y enlaces de los correos) | `https://marketplace-lr.vercel.app` |
| `CORREO_MODO` | `simulado` o `smtp` | `smtp` |
| `CORREO_USUARIO` / `CORREO_CLAVE` | Cuenta y contraseña de aplicación de Gmail | — |
| `PAGOS_MODO` | `simulado` o `stripe` | `simulado` |
| `STRIPE_SECRET_KEY` | Llave de Stripe **modo pruebas** | `sk_test_...` |
| `ALMACENAMIENTO_RUTA` | Carpeta de las fotos subidas | `/var/marketplace/imagenes` |
| `ALMACENAMIENTO_URL` | URL pública de las fotos | `https://api.marketplace-lr.com/imagenes` |
| `ADMIN_CORREO` / `ADMIN_CLAVE` | Administrador inicial | — |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | Logs de consola en JSON | `logstash` |

## 4. Construir y ejecutar

```bash
cd backend
./mvnw -B clean package -DskipTests        # genera target/marketplace-0.0.1-SNAPSHOT.jar
java -jar target/marketplace-0.0.1-SNAPSHOT.jar
```

Para dejarlo como servicio en Linux (systemd), crea `/etc/systemd/system/marketplace.service`:

```ini
[Unit]
Description=API Marketplace LR
After=network.target postgresql.service

[Service]
User=marketplace
WorkingDirectory=/opt/marketplace
EnvironmentFile=/opt/marketplace/.env
ExecStart=/usr/bin/java -jar /opt/marketplace/marketplace.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now marketplace
```

## 5. HTTPS

La API escucha en el puerto 8080 sin HTTPS. En el servidor se pone delante un proxy (Nginx, Caddy) con
certificado de Let's Encrypt que reenvía a `http://localhost:8080`. Con Caddy basta:

```
api.marketplace-lr.com {
    reverse_proxy localhost:8080
}
```

## 6. Verificar

| Qué | Cómo |
|---|---|
| Salud | `GET /salud` → `estado: ok`, `baseDatos: UP` (503 si la base está caída) |
| Documentación | `/swagger-ui.html` |
| Logs | `logs/marketplace.log` en JSON (nivel, fecha, `correlacionId`, `usuarioId`, `operacion`) |
| Métricas | `GET /salud/metricas` con token de administrador |
| Pruebas | `./mvnw test` (usa `marketplace_test`) |

## 7. CI y calidad

- **GitHub Actions** (`.github/workflows/ci.yml`): en cada pull request levanta PostgreSQL 17, corre las
  pruebas y guarda el reporte de cobertura de JaCoCo como artefacto.
- **SonarCloud**: crea el proyecto `sanchavarro_Marketplace-Alojamientos-Producto` en sonarcloud.io, genera un
  token y guárdalo en GitHub → *Settings → Secrets and variables → Actions* como `SONAR_TOKEN`.
  Desde ese momento el análisis corre solo en cada PR.
- **CodeRabbit**: instala la app de CodeRabbit en el repositorio; la configuración está en `.coderabbit.yaml`.

## 8. Pruebas de carga (MKT-104)

Con el backend arriba y un alojamiento ACTIVO con fechas disponibles:

```bash
jmeter -n -t docs/jmeter/marketplace_carga.jmx -Jciudad=Cartagena -Jalojamiento=1 \
       -l resultados.jtl -e -o reporte-carga
```

Abre `reporte-carga/index.html` y revisa la columna **95th pct**:
ESC-01 búsqueda < 2000 ms y ESC-02 cotización < 1000 ms, con 50 usuarios concurrentes.
