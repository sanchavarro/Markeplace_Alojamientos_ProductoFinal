package co.edu.unbosque.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.dto.AlojamientoRequest;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.JwtUtil;

/**
 * Pruebas HTTP de validacion de datos y reglas de negocio del Marketplace.
 * RNF-09 / ESC-09 / T12 (validacion del lado del servidor); RF-11, RF-12,
 * RF-25, RF-38 y RN-06, RN-07.
 *
 * Ejecutar con el perfil "test": utiliza marketplace_test y ddl-auto=create-drop.
 * NO apuntar estas pruebas a marketplace_db (base de la aplicacion).
 */
@AutoConfigureMockMvc
class ValidacionEntradasApiTest extends PruebaIntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtil.generarTokenAcceso(usuario.getId(), usuario.getRol());
    }

    private String json(Object datos) throws Exception {
        return objectMapper.writeValueAsString(datos);
    }

    private AlojamientoRequest alojamientoValido() {
        AlojamientoRequest request = new AlojamientoRequest();
        request.setTitulo("Apartamento valido de prueba");
        request.setDescripcion("Descripcion suficientemente larga para crear un alojamiento valido.");
        request.setCiudad("Bogota");
        request.setDireccion("Calle 100 # 10-20");
        request.setTipo("APARTAMENTO");
        request.setCapacidad(4);
        request.setPrecioNoche(new BigDecimal("150000.00"));
        request.setTarifaLimpieza(new BigDecimal("25000.00"));
        request.setModoReserva(Constantes.MODO_INMEDIATA);
        request.setPoliticaId(politicaRepository.findFirstByTipo(Constantes.POLITICA_FLEXIBLE)
                .orElseThrow().getId());
        return request;
    }

    private void comprobarPublicacionInvalida(AlojamientoRequest request, Usuario anfitrion,
                                               int estadoEsperado, String campoInvalido) throws Exception {
        long antes = alojamientoRepository.count();
        var resultado = mockMvc.perform(post("/alojamientos")
                .header("Authorization", token(anfitrion))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        if (estadoEsperado == 400) {
            resultado.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores." + campoInvalido).exists());
        } else {
            resultado.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.mensaje").exists());
        }
        assertEquals(antes, alojamientoRepository.count(),
                "Una publicacion con datos invalidos no debe quedar guardada");
    }

    private Map<String, Object> cotizacion(Long alojamientoId, LocalDate llegada,
                                           LocalDate salida, int huespedes) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("alojamientoId", alojamientoId);
        datos.put("fechaLlegada", llegada.toString());
        datos.put("fechaSalida", salida.toString());
        datos.put("numHuespedes", huespedes);
        return datos;
    }

    private void cotizacionRechazada(Long alojamientoId, LocalDate llegada,
                                    LocalDate salida, int huespedes, boolean errorDto) throws Exception {
        var resultado = mockMvc.perform(post("/cotizaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(cotizacion(alojamientoId, llegada, salida, huespedes))));
        if (errorDto) {
            resultado.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.numHuespedes").exists());
        } else {
            resultado.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.mensaje").exists());
        }
    }

    @Test
    void unaPublicacionConDatosValidosSeCreaComoBorrador() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        long antes = alojamientoRepository.count();

        mockMvc.perform(post("/alojamientos")
                .header("Authorization", token(anfitrion))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(alojamientoValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value(Constantes.ALOJAMIENTO_BORRADOR));

        assertEquals(antes + 1, alojamientoRepository.count(),
                "Una solicitud valida si debe crear la publicacion");
    }

    @Test
    void rechazaTituloMenorACincoCaracteres() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        AlojamientoRequest request = alojamientoValido();
        request.setTitulo("ABC");
        comprobarPublicacionInvalida(request, anfitrion, 400, "titulo");
    }

    @Test
    void rechazaCapacidadCeroSinGuardarLaPublicacion() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        AlojamientoRequest request = alojamientoValido();
        request.setCapacidad(0);
        comprobarPublicacionInvalida(request, anfitrion, 422, null);
    }

    @Test
    void rechazaPrecioPorNocheCeroSinGuardarLaPublicacion() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        AlojamientoRequest request = alojamientoValido();
        request.setPrecioNoche(BigDecimal.ZERO);
        comprobarPublicacionInvalida(request, anfitrion, 422, null);
    }

    @Test
    void rechazaTarifaDeLimpiezaNegativa() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        AlojamientoRequest request = alojamientoValido();
        request.setTarifaLimpieza(new BigDecimal("-1000"));
        comprobarPublicacionInvalida(request, anfitrion, 400, "tarifaLimpieza");
    }

    @Test
    void rechazaTipoDeAlojamientoDesconocido() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        AlojamientoRequest request = alojamientoValido();
        request.setTipo("CASTILLO_ESPACIAL");
        comprobarPublicacionInvalida(request, anfitrion, 422, null);
    }

    @Test
    void rechazaModoDeReservaNoAdmitido() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        AlojamientoRequest request = alojamientoValido();
        request.setModoReserva("SIN_CONTROL");
        comprobarPublicacionInvalida(request, anfitrion, 422, null);
    }

    @Test
    void rechazaCotizacionConEntradaYSalidaEnLaMismaFecha() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        LocalDate llegada = LocalDate.now().plusDays(10);
        cotizacionRechazada(alojamiento.getId(), llegada, llegada, 2, false);
    }

    @Test
    void rechazaCotizacionConSalidaAnteriorALaLlegada() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        LocalDate llegada = LocalDate.now().plusDays(10);
        cotizacionRechazada(alojamiento.getId(), llegada, llegada.minusDays(1), 2, false);
    }

    @Test
    void rechazaCotizacionConLlegadaEnElPasado() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        LocalDate llegada = LocalDate.now().minusDays(1);
        cotizacionRechazada(alojamiento.getId(), llegada, llegada.plusDays(2), 2, false);
    }

    @Test
    void rechazaCotizacionQueSuperaLaCapacidadDelAlojamiento() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        LocalDate llegada = LocalDate.now().plusDays(10);
        cotizacionRechazada(alojamiento.getId(), llegada, llegada.plusDays(2), 5, false);
    }

    @Test
    void rechazaCotizacionConCeroHuespedes() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        LocalDate llegada = LocalDate.now().plusDays(10);
        cotizacionRechazada(alojamiento.getId(), llegada, llegada.plusDays(2), 0, true);
    }

    @Test
    void rechazaResenasConCalificacionesFueraDelRangoDeUnoACinco() throws Exception {
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);
        for (int calificacion : new int[]{0, 6}) {
            mockMvc.perform(post("/resenas")
                    .header("Authorization", token(huesped))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("reservaId", 999999999L,
                            "calificacion", calificacion,
                            "comentario", "Intento de calificacion invalida"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.calificacion").exists());
        }
    }
}
