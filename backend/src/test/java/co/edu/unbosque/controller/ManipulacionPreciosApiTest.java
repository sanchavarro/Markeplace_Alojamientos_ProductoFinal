package co.edu.unbosque.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.Pago;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.JwtUtil;

/**
 * RNF-06 / ESC-06 / ASR de seguridad: el total de una reserva se calcula en el servidor.
 * Un cliente no puede rebajar el importe agregando o alterando campos monetarios en el JSON.
 *
 * Pruebas de integracion HTTP + base de datos (marketplace_test, NO marketplace_db).
 * El perfil de pruebas configura pagos.modo=simulado: NO realiza cobros reales.
 */
@AutoConfigureMockMvc
class ManipulacionPreciosApiTest extends PruebaIntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;

    private Map<String, Object> solicitudBase(Alojamiento alojamiento, LocalDate llegada) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("alojamientoId", alojamiento.getId());
        datos.put("fechaLlegada", llegada.toString());
        datos.put("fechaSalida", llegada.plusDays(3).toString());
        datos.put("numHuespedes", 2);
        return datos;
    }

    private String json(Object datos) throws Exception {
        return objectMapper.writeValueAsString(datos);
    }

    private JsonNode cotizar(Map<String, Object> datos) throws Exception {
        String respuesta = mockMvc.perform(post("/cotizaciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(datos)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta);
    }

    private JsonNode reservar(Usuario huesped, Map<String, Object> datos) throws Exception {
        String token = jwtUtil.generarTokenAcceso(huesped.getId(), huesped.getRol());
        String respuesta = mockMvc.perform(post("/reservas")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(datos)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta);
    }

    private void assertMismoMonto(JsonNode esperado, String campo, JsonNode real) {
        BigDecimal montoEsperado = new BigDecimal(esperado.path(campo).asText());
        BigDecimal montoReal = new BigDecimal(real.path(campo).asText());
        assertEquals(0, montoEsperado.compareTo(montoReal),
                "El campo " + campo + " debe calcularse en el servidor");
    }

    private void verificarPagoPersistido(JsonNode cotizacion, JsonNode respuesta) {
        Long reservaId = respuesta.path("id").asLong();
        Reserva reserva = reservaRepository.findById(reservaId).orElseThrow();
        Pago pago = pagoRepository.findFirstByReservaIdAndEstado(reservaId, Constantes.PAGO_APROBADO)
                .orElseThrow();

        assertEquals(Constantes.RESERVA_CONFIRMADA, reserva.getEstado());
        assertEquals(Constantes.PAGO_APROBADO, respuesta.path("estadoPago").asText());
        assertEquals(0, new BigDecimal(cotizacion.path("total").asText())
                .compareTo(reserva.getTotal()), "El total de la reserva debe ser el del servidor");
        assertEquals(0, reserva.getTotal().compareTo(pago.getMonto()),
                "El pago registrado debe tener el mismo importe que la reserva");
        assertMismoMonto(cotizacion, "total", respuesta);
        assertEquals(0, new BigDecimal(cotizacion.path("precioBase").asText())
                .compareTo(reserva.getPrecioBase()));
        assertEquals(0, new BigDecimal(cotizacion.path("tarifaLimpieza").asText())
                .compareTo(reserva.getTarifaLimpieza()));
        assertEquals(0, new BigDecimal(cotizacion.path("tarifaServicio").asText())
                .compareTo(reserva.getTarifaServicio()));
        assertEquals(0, new BigDecimal(cotizacion.path("descuento").asText())
                .compareTo(reserva.getDescuento()));
    }

    @Test
    void cotizacionIgnoraPreciosYDescuentosManipulados() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Map<String, Object> datos = solicitudBase(alojamiento, LocalDate.now().plusDays(12));
        JsonNode cotizacionValida = cotizar(datos);

        // Un usuario agrega campos que NO forman parte del contrato CotizacionRequest.
        datos.put("precioNoche", 1);
        datos.put("precioBase", 1);
        datos.put("tarifaLimpieza", 0);
        datos.put("tarifaServicio", 0);
        datos.put("descuento", 999999999);
        datos.put("total", 1);

        JsonNode cotizacionManipulada = cotizar(datos);
        for (String campo : new String[] {"precioNoche", "precioBase", "tarifaLimpieza",
                "tarifaServicio", "descuento", "total"}) {
            assertMismoMonto(cotizacionValida, campo, cotizacionManipulada);
        }
        assertTrue(new BigDecimal(cotizacionManipulada.path("total").asText())
                .compareTo(BigDecimal.ONE) > 0);
    }

    @Test
    void reservaNoAceptaTotalDeUnPesoNiDescuentoFalso() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);
        Map<String, Object> datos = solicitudBase(alojamiento, LocalDate.now().plusDays(17));
        JsonNode cotizacionReal = cotizar(datos);

        // Simula editar el body del POST /reservas para pagar COP 1.
        datos.put("metodoPago", "pm_card_visa");
        datos.put("precioBase", 1);
        datos.put("tarifaLimpieza", 0);
        datos.put("tarifaServicio", 0);
        datos.put("descuento", 999999999);
        datos.put("total", 1);

        JsonNode reserva = reservar(huesped, datos);
        verificarPagoPersistido(cotizacionReal, reserva);
    }

    @Test
    void reservaIgnoraImportesNegativosEnviadosDesdeElCliente() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);
        Map<String, Object> datos = solicitudBase(alojamiento, LocalDate.now().plusDays(23));
        JsonNode cotizacionReal = cotizar(datos);

        // Un importe negativo no debe convertirse en un descuento o cobro negativo.
        datos.put("metodoPago", "pm_card_visa");
        datos.put("precioNoche", -100);
        datos.put("tarifaServicio", -50000);
        datos.put("total", -1000000);

        JsonNode reserva = reservar(huesped, datos);
        verificarPagoPersistido(cotizacionReal, reserva);
    }
}
