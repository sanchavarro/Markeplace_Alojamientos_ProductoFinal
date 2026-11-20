package co.edu.unbosque.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.dto.AlojamientoRequest;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.JwtUtil;
import jakarta.servlet.http.Cookie;

/**
 * Pruebas de la configuracion de seguridad HTTP del Marketplace.
 *
 * El proyecto usa JWT enviado explicitamente en Authorization y sesiones STATELESS.
 * Por esa razon CSRF esta desactivado en SecurityConfig; estas pruebas documentan
 * y verifican la premisa, NO pretenden demostrar que CSRF este activado.
 *
 * CORS controla solicitudes hechas por navegadores, NO reemplaza autenticacion.
 *
 * Usa la base marketplace_test (perfil test; ddl-auto=create-drop).
 * No ejecutar contra marketplace_db ni modificar application-test.properties.
 */
@AutoConfigureMockMvc
class SeguridadCsrfCabecerasTest extends PruebaIntegracionBase {

    private static final String ORIGEN_PERMITIDO = "http://localhost:5173";
    private static final String ORIGEN_NO_PERMITIDO = "https://sitio-ajeno.example";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtUtil.generarTokenAcceso(usuario.getId(), usuario.getRol());
    }

    private AlojamientoRequest alojamientoValido() {
        AlojamientoRequest solicitud = new AlojamientoRequest();
        solicitud.setTitulo("Apartamento de prueba HTTP");
        solicitud.setDescripcion("Descripción suficientemente extensa para crear la publicación.");
        solicitud.setCiudad("Bogota");
        solicitud.setDireccion("Calle 100 # 10-20");
        solicitud.setTipo("APARTAMENTO");
        solicitud.setCapacidad(2);
        solicitud.setPrecioNoche(new BigDecimal("180000.00"));
        solicitud.setTarifaLimpieza(BigDecimal.ZERO);
        solicitud.setModoReserva(Constantes.MODO_INMEDIATA);
        solicitud.setPoliticaId(politicaRepository.findFirstByTipo(Constantes.POLITICA_FLEXIBLE)
                .orElseThrow().getId());
        return solicitud;
    }

    /**
     * La API JWT admite una accion de escritura autenticada sin token CSRF.
     * Comprueba tambien que el backend aplico la operacion, no solo el status HTTP.
     */
    @Test
    void jwtValidoPermiteCrearAlojamientoSinTokenCsrf() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        long cantidadAnterior = alojamientoRepository.count();

        mockMvc.perform(post("/alojamientos")
                    .header(HttpHeaders.AUTHORIZATION, bearer(anfitrion))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(alojamientoValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value(Constantes.ALOJAMIENTO_BORRADOR));

        assertEquals(cantidadAnterior + 1, alojamientoRepository.count(),
                "La solicitud autenticada, sin token CSRF, debe crear un borrador");
    }

    /** Un encabezado CSRF inventado NO reemplaza al JWT obligatorio. */
    @Test
    void tokenCsrfInventadoNoSustituyeAutenticacion() throws Exception {
        long cantidadAnterior = alojamientoRepository.count();

        mockMvc.perform(post("/alojamientos")
                    .header("X-CSRF-TOKEN", "token-inventado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());

        assertEquals(cantidadAnterior, alojamientoRepository.count(),
                "La solicitud sin JWT no debe crear alojamientos");
    }

    /**
     * El filtro JWT no lee una cookie llamada Authorization: requiere el header.
     * Esta comprobacion es importante para justificar CSRF deshabilitado.
     */
    @Test
    void jwtDentroDeCookieNoAutenticaAlUsuario() throws Exception {
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);
        String token = bearer(huesped).substring("Bearer ".length());

        mockMvc.perform(get("/usuarios/perfil")
                    .cookie(new Cookie("Authorization", "Bearer " + token)))
                .andExpect(status().isUnauthorized());
    }

    /** La solicitud protegida con JWT no debe abrir una sesion HTTP de servidor. */
    @Test
    void solicitudConJwtNoCreaSesionHttpNiCookieDeSesion() throws Exception {
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);

        var resultado = mockMvc.perform(get("/usuarios/perfil")
                    .header(HttpHeaders.AUTHORIZATION, bearer(huesped)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        assertNull(resultado.getRequest().getSession(false),
                "Una API stateless no debe crear HttpSession");
    }

    /** CORS: el preflight del frontend declarado debe estar permitido. */
    @Test
    void corsPermitePreflightDelFrontendConfigurado() throws Exception {
        mockMvc.perform(options("/alojamientos")
                    .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO));
    }

    /** CORS: el preflight desde otro origen no recibe autorizacion del navegador. */
    @Test
    void corsRechazaPreflightDeOrigenNoPermitido() throws Exception {
        mockMvc.perform(options("/alojamientos")
                    .header(HttpHeaders.ORIGIN, ORIGEN_NO_PERMITIDO)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /** Evita que el navegador interprete contenido usando MIME sniffing. */
    @Test
    void cabeceraXContentTypeOptionsEvitaMimeSniffing() throws Exception {
        mockMvc.perform(get("/salud"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    /** Proteccion predeterminada de Spring Security contra clickjacking. */
    @Test
    void cabeceraXFrameOptionsImpideEmbeberLaApi() throws Exception {
        mockMvc.perform(get("/salud"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    /** Los datos privados no deben quedar cacheados por defecto en el navegador. */
    @Test
    void perfilAutenticadoTienePoliticaNoStore() throws Exception {
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);

        mockMvc.perform(get("/usuarios/perfil")
                    .header(HttpHeaders.AUTHORIZATION, bearer(huesped)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        org.hamcrest.Matchers.containsString("no-store")));
    }

    /** Spring Security solo envia HSTS cuando la solicitud se recibe por HTTPS. */
    @Test
    void httpsIncluyeHsts() throws Exception {
        mockMvc.perform(get("/salud").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security",
                        org.hamcrest.Matchers.containsString("max-age=")));
    }

    /** No se debe declarar HSTS para respuestas servidas por HTTP no cifrado. */
    @Test
    void httpNoIncluyeHsts() throws Exception {
        mockMvc.perform(get("/salud").secure(false))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}
