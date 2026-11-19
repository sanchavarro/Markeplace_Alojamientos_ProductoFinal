package co.edu.unbosque.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.utils.Constantes;

/**
 * ESC-09 / RNF-09. Comprueba que campos con contenido parecido a HTML/JavaScript
 * se devuelven como DATOS dentro de respuestas JSON, no como documentos HTML.
 *
 * IMPORTANTE: estas pruebas son del backend. NO verifican que el navegador
 * neutralice el XSS. Deben complementarse con una prueba de la interfaz web.
 * Utiliza marketplace_test por heredar de PruebaIntegracionBase.
 */
@AutoConfigureMockMvc
class XssApiTest extends PruebaIntegracionBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void descripcionConScriptSeDevuelveComoDatoJson() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        String payload = "Prueba de seguridad <script>alert('XSS')</script> contenido.";
        alojamiento.setDescripcion(payload);
        alojamientoRepository.save(alojamiento);

        mockMvc.perform(get("/alojamientos/{id}", alojamiento.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.descripcion").value(payload));
    }

    @Test
    void reglasConEventoHtmlSeDevuelvenComoDatoJson() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        String payload = "Normas de seguridad <img src=x onerror=alert('XSS')> del alojamiento";
        alojamiento.setReglas(payload);
        alojamientoRepository.save(alojamiento);

        mockMvc.perform(get("/alojamientos/{id}", alojamiento.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reglas").value(payload));
    }
}
