package co.edu.unbosque.utils;

import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantilla HTML de los correos de la plataforma (misma identidad visual del frontend: blanco, #F5F5F7,
 * texto #1D1D1F, botones redondeados).
 *
 * Está hecha con tablas y estilos en línea porque es lo único que respetan Gmail, Outlook y Apple Mail.
 * Todo el texto dinámico se escapa para evitar inyección de HTML. Además del HTML se genera siempre
 * una versión en texto plano para los clientes que no muestran HTML.
 */
public final class PlantillaCorreo {

    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    private PlantillaCorreo() {
    }

    /** Correo listo para enviar: asunto, texto plano y HTML. */
    public record Correo(String asunto, String texto, String html) {
    }

    /** Llamado a la acción (botón) de cada tipo de evento. */
    record Accion(String etiqueta, String url) {
    }

    /**
     * Arma el correo de un evento del outbox.
     *
     * @param tipoEvento  tipo del evento (Constantes.EVENTO_*)
     * @param nombre      nombre del destinatario
     * @param correo      correo del destinatario (para el enlace de verificación)
     * @param rol         rol del destinatario (decide a qué pantalla lleva el botón)
     * @param titulo      asunto/título del evento
     * @param mensaje     mensaje del evento
     * @param enlace      enlace propio del evento (recuperación de clave), o null
     * @param urlFrontend URL base del frontend (app.frontend.url)
     */
    public static Correo construir(String tipoEvento, String nombre, String correo, String rol, String titulo,
                                   String mensaje, String enlace, String urlFrontend) {
        String base = urlFrontend == null ? "" : urlFrontend.replaceAll("/+$", "");
        String codigo = Constantes.EVENTO_VERIFICACION_CORREO.equals(tipoEvento) ? extraerCodigo(mensaje) : null;
        Accion accion = accion(tipoEvento, rol, correo, enlace, base);
        String asunto = (titulo == null || titulo.isBlank() ? "Novedades de tu cuenta" : titulo) + " · Marketplace";

        StringBuilder texto = new StringBuilder();
        texto.append("Hola ").append(nombre).append(",\n\n").append(mensaje).append("\n");
        if (accion != null) {
            texto.append("\n").append(accion.etiqueta()).append(": ").append(accion.url()).append("\n");
        }
        texto.append("\n¿Necesitas ayuda? Escríbenos respondiendo este correo.\nMarketplace de Alojamientos");

        String html = html(titulo, nombre, mensaje, codigo, accion, base, colorDe(tipoEvento));
        return new Correo(asunto, texto.toString(), html);
    }

    static String extraerCodigo(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        Matcher m = CODIGO.matcher(mensaje);
        return m.find() ? m.group(1) : null;
    }

    static Accion accion(String tipo, String rol, String correo, String enlace, String base) {
        if (enlace != null && !enlace.isBlank()) {
            return new Accion(Constantes.EVENTO_RECUPERACION_CLAVE.equals(tipo) ? "Crear una nueva clave" : "Abrir enlace", enlace);
        }
        boolean anfitrion = Constantes.ROL_ANFITRION.equals(rol);
        boolean admin = Constantes.ROL_ADMINISTRADOR.equals(rol);
        if (tipo == null) {
            return new Accion("Ir a Marketplace", base + "/");
        }
        return switch (tipo) {
            case Constantes.EVENTO_VERIFICACION_CORREO ->
                    new Accion("Confirmar mi correo", base + "/verificar-correo?correo=" + urlEncode(correo));
            case Constantes.EVENTO_USUARIO_REGISTRADO ->
                    anfitrion ? new Accion("Publicar mi alojamiento", base + "/anfitrion/alojamientos/nuevo")
                              : new Accion("Explorar alojamientos", base + "/explorar");
            case Constantes.EVENTO_RESERVA_POR_APROBAR -> new Accion("Revisar la solicitud", base + "/anfitrion/reservas");
            case Constantes.EVENTO_RESERVA_APROBADA -> new Accion("Pagar mi reserva", base + "/mis-reservas");
            case Constantes.EVENTO_PAGO_FALLIDO -> new Accion("Intentar de nuevo", base + "/mis-reservas");
            case Constantes.EVENTO_RESERVA_CONFIRMADA, Constantes.EVENTO_RESERVA_CANCELADA ->
                    anfitrion ? new Accion("Ver reservas recibidas", base + "/anfitrion/reservas")
                              : new Accion("Ver mis viajes", base + "/mis-reservas");
            case Constantes.EVENTO_PUBLICACION_ESTADO ->
                    anfitrion ? new Accion("Ver mis alojamientos", base + "/anfitrion")
                              : new Accion("Ver mis viajes", base + "/mis-reservas");
            case Constantes.EVENTO_REPORTE_LISTO -> new Accion("Descargar el reporte", base + "/admin/reportes");
            case Constantes.EVENTO_USUARIO_ESTADO -> new Accion("Ir a mi cuenta", base + "/login");
            default -> new Accion(admin ? "Ir al panel" : "Ir a Marketplace", base + (admin ? "/admin" : "/"));
        };
    }

    /** Color de acento por tipo de evento (éxito, alerta o neutro). */
    static String colorDe(String tipo) {
        if (tipo == null) {
            return "#1D1D1F";
        }
        return switch (tipo) {
            case Constantes.EVENTO_RESERVA_CONFIRMADA, Constantes.EVENTO_RESERVA_APROBADA, Constantes.EVENTO_REPORTE_LISTO -> "#1E8E3E";
            case Constantes.EVENTO_PAGO_FALLIDO, Constantes.EVENTO_RESERVA_CANCELADA -> "#E0475B";
            case Constantes.EVENTO_RESERVA_POR_APROBAR -> "#C77700";
            default -> "#1D1D1F";
        };
    }

    private static String html(String titulo, String nombre, String mensaje, String codigo, Accion accion,
                               String base, String acento) {
        String fuente = "-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif";
        StringBuilder h = new StringBuilder(4096);
        h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
         .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
         .append("<title>").append(esc(titulo)).append("</title></head>")
         .append("<body style=\"margin:0;padding:0;background:#F5F5F7;\">")
         // Texto de vista previa (lo que muestra la bandeja de entrada junto al asunto)
         .append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">").append(esc(resumen(mensaje))).append("</div>")
         .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#F5F5F7;padding:32px 12px;\"><tr><td align=\"center\">")
         .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;\">")
         // Encabezado con logo
         .append("<tr><td style=\"padding:0 8px 20px 8px;font-family:").append(fuente).append(";\">")
         .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>")
         .append("<td style=\"background:#1D1D1F;border-radius:10px;width:34px;height:34px;text-align:center;color:#FFFFFF;font-size:18px;font-weight:700;\">&#8962;</td>")
         .append("<td style=\"padding-left:10px;font-size:18px;font-weight:600;color:#1D1D1F;letter-spacing:-0.3px;\">Marketplace</td>")
         .append("</tr></table></td></tr>")
         // Tarjeta principal
         .append("<tr><td style=\"background:#FFFFFF;border-radius:24px;padding:36px 32px;font-family:").append(fuente)
         .append(";box-shadow:0 4px 16px rgba(0,0,0,0.06);\">")
         .append("<div style=\"width:44px;height:4px;border-radius:4px;background:").append(acento).append(";margin-bottom:22px;\"></div>")
         .append("<h1 style=\"margin:0 0 14px 0;font-size:24px;line-height:1.25;color:#1D1D1F;font-weight:600;letter-spacing:-0.4px;\">")
         .append(esc(titulo)).append("</h1>")
         .append("<p style=\"margin:0 0 12px 0;font-size:16px;line-height:1.6;color:#424245;\">Hola ").append(esc(nombre)).append(",</p>")
         .append("<p style=\"margin:0 0 24px 0;font-size:16px;line-height:1.6;color:#424245;\">").append(esc(mensaje).replace("\n", "<br>")).append("</p>");
        if (codigo != null) {
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 24px 0;\"><tr>")
             .append("<td align=\"center\" style=\"background:#F5F5F7;border-radius:16px;padding:20px;\">")
             .append("<div style=\"font-size:12px;color:#6E6E73;text-transform:uppercase;letter-spacing:1.5px;margin-bottom:8px;\">Tu código</div>")
             .append("<div style=\"font-family:'SF Mono',Menlo,Consolas,monospace;font-size:34px;font-weight:700;letter-spacing:10px;color:#1D1D1F;\">")
             .append(codigo).append("</div></td></tr></table>");
        }
        if (accion != null) {
            h.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 8px 0;\"><tr>")
             .append("<td style=\"border-radius:999px;background:#1D1D1F;\">")
             .append("<a href=\"").append(esc(accion.url())).append("\" style=\"display:inline-block;padding:14px 28px;font-size:15px;font-weight:600;color:#FFFFFF;text-decoration:none;border-radius:999px;\">")
             .append(esc(accion.etiqueta())).append("</a></td></tr></table>")
             .append("<p style=\"margin:16px 0 0 0;font-size:12px;line-height:1.5;color:#86868B;\">Si el botón no funciona, copia este enlace en tu navegador:<br>")
             .append("<a href=\"").append(esc(accion.url())).append("\" style=\"color:#0071E3;word-break:break-all;\">").append(esc(accion.url())).append("</a></p>");
        }
        h.append("</td></tr>")
         // Pie
         .append("<tr><td style=\"padding:24px 8px 0 8px;font-family:").append(fuente).append(";font-size:12px;line-height:1.6;color:#86868B;text-align:center;\">")
         .append("¿Necesitas algo? Responde este correo y el equipo de Marketplace te ayuda.<br>")
         .append("<a href=\"").append(esc(base)).append("/ayuda\" style=\"color:#6E6E73;\">Centro de ayuda</a> &nbsp;·&nbsp; ")
         .append("<a href=\"").append(esc(base)).append("/explorar\" style=\"color:#6E6E73;\">Explorar alojamientos</a><br><br>")
         .append("© ").append(Year.now().getValue()).append(" Marketplace de Alojamientos · Recibes este correo por la actividad de tu cuenta.")
         .append("</td></tr></table></td></tr></table></body></html>");
        return h.toString();
    }

    private static String resumen(String mensaje) {
        if (mensaje == null) {
            return "";
        }
        return mensaje.length() > 110 ? mensaje.substring(0, 107) + "..." : mensaje;
    }

    /** Escapa los caracteres especiales de HTML. */
    static String esc(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(valor.length());
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String urlEncode(String valor) {
        return java.net.URLEncoder.encode(valor == null ? "" : valor, java.nio.charset.StandardCharsets.UTF_8);
    }
}
