package co.edu.unbosque.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Captcha propio (sin servicios externos ni claves) para el registro de usuarios.
 *
 * - Genera un texto aleatorio de 5 caracteres sin símbolos ambiguos (0/O, 1/I/L) y lo dibuja como SVG
 *   con rotación, desplazamiento y líneas de ruido.
 * - La respuesta correcta solo vive en el servidor (memoria), asociada a un identificador UUID.
 * - Cada captcha es de un solo uso (se elimina al validarlo, acierte o no) y vence en 5 minutos.
 * - El almacén tiene un tope de entradas para que no pueda crecer sin límite.
 */
@Component
public class ServicioCaptcha {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD = 5;
    private static final long VIGENCIA_SEGUNDOS = 300;
    private static final int MAXIMO_EN_MEMORIA = 10_000;

    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Desafio> desafios = new ConcurrentHashMap<>();

    @Value("${seguridad.captcha.habilitado:true}")
    private boolean habilitado;

    private record Desafio(String respuesta, Instant expira) {
    }

    public boolean estaHabilitado() {
        return habilitado;
    }

    /** Crea un captcha nuevo: { captchaId, imagen (data URI SVG), expiraEnSegundos }. */
    public Map<String, Object> generar() {
        limpiarVencidos();
        if (desafios.size() >= MAXIMO_EN_MEMORIA) {
            throw new ServicioNoDisponibleException("Hay demasiadas solicitudes de captcha. Intenta en un momento.");
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < LONGITUD; i++) {
            texto.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        String id = UUID.randomUUID().toString();
        desafios.put(id, new Desafio(texto.toString(), Instant.now().plusSeconds(VIGENCIA_SEGUNDOS)));

        String svg = dibujar(texto.toString());
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("captchaId", id);
        respuesta.put("imagen", "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)));
        respuesta.put("expiraEnSegundos", VIGENCIA_SEGUNDOS);
        return respuesta;
    }

    /**
     * Valida la respuesta (sin distinguir mayúsculas ni espacios). El captcha se consume siempre,
     * para que no se pueda probar por fuerza bruta con el mismo identificador.
     */
    public void validar(String captchaId, String respuesta) {
        if (!habilitado) {
            return;
        }
        if (captchaId == null || captchaId.isBlank() || respuesta == null || respuesta.isBlank()) {
            throw new ReglaNegocioException("Resuelve el captcha para continuar.");
        }
        Desafio desafio = desafios.remove(captchaId.trim());
        if (desafio == null || desafio.expira().isBefore(Instant.now())) {
            throw new ReglaNegocioException("El captcha venció. Genera uno nuevo.");
        }
        String limpia = respuesta.replaceAll("\\s", "").toUpperCase();
        if (!desafio.respuesta().equals(limpia)) {
            throw new ReglaNegocioException("El texto del captcha no coincide. Intenta con uno nuevo.");
        }
    }

    private void limpiarVencidos() {
        Instant ahora = Instant.now();
        desafios.entrySet().removeIf(e -> e.getValue().expira().isBefore(ahora));
    }

    private String dibujar(String texto) {
        int ancho = 180;
        int alto = 60;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(ancho).append("' height='").append(alto)
                .append("' viewBox='0 0 ").append(ancho).append(' ').append(alto).append("'>");
        svg.append("<rect width='100%' height='100%' rx='12' fill='#F5F5F7'/>");
        for (int i = 0; i < 6; i++) {
            svg.append("<path d='M").append(azar.nextInt(ancho)).append(' ').append(azar.nextInt(alto))
                    .append(" Q").append(azar.nextInt(ancho)).append(' ').append(azar.nextInt(alto))
                    .append(' ').append(azar.nextInt(ancho)).append(' ').append(azar.nextInt(alto))
                    .append("' stroke='").append(color(150)).append("' stroke-width='1.4' fill='none' opacity='0.7'/>");
        }
        for (int i = 0; i < 40; i++) {
            svg.append("<circle cx='").append(azar.nextInt(ancho)).append("' cy='").append(azar.nextInt(alto))
                    .append("' r='1.2' fill='").append(color(170)).append("'/>");
        }
        int paso = (ancho - 30) / texto.length();
        for (int i = 0; i < texto.length(); i++) {
            int x = 20 + i * paso + azar.nextInt(8);
            int y = 40 + azar.nextInt(10) - 5;
            int giro = azar.nextInt(50) - 25;
            int tamano = 26 + azar.nextInt(8);
            svg.append("<text x='").append(x).append("' y='").append(y).append("' font-size='").append(tamano)
                    .append("' font-family='Georgia, serif' font-weight='bold' fill='").append(color(90))
                    .append("' transform='rotate(").append(giro).append(' ').append(x).append(' ').append(y)
                    .append(")'>").append(texto.charAt(i)).append("</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    /** Color gris azulado aleatorio por debajo del brillo máximo indicado. */
    private String color(int maximo) {
        int r = 20 + azar.nextInt(Math.max(1, maximo - 20));
        int g = 20 + azar.nextInt(Math.max(1, maximo - 20));
        int b = 40 + azar.nextInt(Math.max(1, maximo - 20));
        return String.format("#%02X%02X%02X", Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }
}
