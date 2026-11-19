package co.edu.unbosque.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class CorsConfigTest {

    private CorsConfiguration configuracion(String frontend, String adicionales) {
        SecurityConfig seguridad = new SecurityConfig();
        ReflectionTestUtils.setField(seguridad, "frontendUrl", frontend);
        ReflectionTestUtils.setField(seguridad, "origenesAdicionales", adicionales);
        UrlBasedCorsConfigurationSource fuente = (UrlBasedCorsConfigurationSource) seguridad.corsConfigurationSource();
        return fuente.getCorsConfigurations().get("/**");
    }

    @Test
    void permiteElFrontendYLosOrigenesAdicionales() {
        CorsConfiguration cors = configuracion("https://www.marketplacelr.site",
                " https://marketplacelr.site , ,https://*.azurestaticapps.net");

        assertEquals("https://www.marketplacelr.site", cors.checkOrigin("https://www.marketplacelr.site"));
        assertEquals("https://marketplacelr.site", cors.checkOrigin("https://marketplacelr.site"));
        assertEquals("https://app.azurestaticapps.net", cors.checkOrigin("https://app.azurestaticapps.net"));
        assertNull(cors.checkOrigin("https://sitio-desconocido.com"));
    }

    @Test
    void sinOrigenesAdicionalesSoloPermiteElFrontend() {
        CorsConfiguration cors = configuracion("http://localhost:5173", "");

        assertEquals("http://localhost:5173", cors.checkOrigin("http://localhost:5173"));
        assertNull(cors.checkOrigin("https://marketplacelr.site"));
    }
}
