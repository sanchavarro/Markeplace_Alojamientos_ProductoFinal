package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Captcha del registro: imagen SVG, un solo uso y validación en el servidor. */
class ServicioCaptchaTest {

    private ServicioCaptcha servicio(boolean habilitado) {
        ServicioCaptcha s = new ServicioCaptcha();
        ReflectionTestUtils.setField(s, "habilitado", habilitado);
        return s;
    }

    @Test
    void generaUnaImagenSvgConIdentificador() {
        Map<String, Object> c = servicio(true).generar();
        assertNotNull(c.get("captchaId"));
        assertTrue(String.valueOf(c.get("imagen")).startsWith("data:image/svg+xml;base64,"));
    }

    @Test
    void rechazaRespuestasIncorrectasYElCaptchaEsDeUnSoloUso() {
        ServicioCaptcha s = servicio(true);
        String id = (String) s.generar().get("captchaId");
        assertThrows(ReglaNegocioException.class, () -> s.validar(id, "!!!!!"));
        // El primer intento lo consumió: aunque se acierte, ya no existe.
        assertThrows(ReglaNegocioException.class, () -> s.validar(id, "ABCDE"));
    }

    @Test
    void aceptaLaRespuestaCorrectaSinImportarMayusculasNiEspacios() throws Exception {
        ServicioCaptcha s = servicio(true);
        String id = (String) s.generar().get("captchaId");
        Map<?, ?> desafios = (Map<?, ?>) ReflectionTestUtils.getField(s, "desafios");
        Object desafio = desafios.get(id);
        java.lang.reflect.Method respuesta = desafio.getClass().getDeclaredMethod("respuesta");
        respuesta.setAccessible(true);
        String correcta = (String) respuesta.invoke(desafio);
        assertDoesNotThrow(() -> s.validar(id, " " + correcta.toLowerCase() + " "));
        assertTrue(s.estaHabilitado());
    }

    @Test
    void exigeRespuestaCuandoEstaHabilitado() {
        assertThrows(ReglaNegocioException.class, () -> servicio(true).validar(null, null));
    }

    @Test
    void noValidaNadaCuandoEstaDeshabilitado() {
        assertDoesNotThrow(() -> servicio(false).validar(null, null));
    }
}
