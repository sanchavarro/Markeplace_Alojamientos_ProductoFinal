package co.edu.unbosque.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Funciones de apoyo usadas en varias capas.
 */
public final class Utilidad {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private Utilidad() {
    }

    public static String generarIdCorrelacion() {
        return UUID.randomUUID().toString();
    }

    /** Id del usuario autenticado; lo pone el filtro JWT como principal. */
    public static Long idUsuarioActual(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof Long)) {
            throw new CredencialesInvalidasException("Debes iniciar sesión.");
        }
        return (Long) auth.getPrincipal();
    }

    /** Igual que idUsuarioActual, pero devuelve null si la petición es anónima (rutas públicas). */
    public static Long idUsuarioOpcional(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    public static String rolActual(Authentication auth) {
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
    }

    public static long calcularNoches(LocalDate llegada, LocalDate salida) {
        return ChronoUnit.DAYS.between(llegada, salida);
    }

    /** HU-25: la salida debe ser posterior a la llegada y la llegada no puede estar en el pasado. */
    public static void validarFechas(LocalDate llegada, LocalDate salida) {
        if (llegada == null || salida == null) {
            throw new ReglaNegocioException("Debes indicar la fecha de llegada y la de salida.");
        }
        if (!salida.isAfter(llegada)) {
            throw new ReglaNegocioException("La fecha de salida debe ser posterior a la fecha de llegada.");
        }
        if (llegada.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede reservar ni cotizar en fechas pasadas.");
        }
        if (calcularNoches(llegada, salida) > Constantes.NOCHES_MAXIMAS) {
            throw new ReglaNegocioException("La estadía máxima es de " + Constantes.NOCHES_MAXIMAS + " noches.");
        }
    }

    public static String aJson(Object objeto) {
        try {
            return MAPPER.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir a JSON.", e);
        }
    }

    public static <T> T desdeJson(String json, Class<T> tipo) {
        try {
            return MAPPER.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el JSON.", e);
        }
    }

    public static String detalle(Object... claveValor) {
        java.util.LinkedHashMap<String, Object> mapa = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < claveValor.length; i += 2) {
            mapa.put(String.valueOf(claveValor[i]), claveValor[i + 1]);
        }
        return aJson(mapa);
    }

    /** Oculta parte del correo para no dejar datos personales completos en los logs: s***@gmail.com */
    public static String ocultarCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "***";
        }
        int arroba = correo.indexOf('@');
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }

    public static Map<String, String> mensaje(String texto) {
        return Map.of("mensaje", texto);
    }

    public static int normalizarTamanoPagina(Integer tamano, int defecto, int maximo) {
        if (tamano == null || tamano < 1) {
            return defecto;
        }
        return Math.min(tamano, maximo);
    }
}
