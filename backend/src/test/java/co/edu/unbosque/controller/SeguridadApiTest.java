package co.edu.unbosque.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import co.edu.unbosque.PruebaIntegracionBase;

/**
 * Pruebas de la API completa con MockMvc: seguridad por rol (HU-03, ADR-05), validaciones,
 * salud (HU-49) y documentación Swagger (MKT-102).
 */
@AutoConfigureMockMvc
class SeguridadApiTest extends PruebaIntegracionBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void saludEsPublicaYReportaLaBaseDeDatos() throws Exception {
        mockMvc.perform(get("/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseDatos").value("UP"));
    }

    @Test
    void sinTokenRespondeNoAutorizado() throws Exception {
        mockMvc.perform(get("/reservas/mias")).andExpect(status().isUnauthorized());
    }

    @Test
    void huespedNoEntraARutasDeAdministrador() throws Exception {
        String correo = "huesped-" + UUID.randomUUID() + "@prueba.com";
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Huésped API\",\"correo\":\"" + correo
                                + "\",\"clave\":\"Huesped2026*\",\"rol\":\"HUESPED\"}"))
                .andExpect(status().isCreated());
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"clave\":\"Huesped2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(correo));
    }

    @Test
    void elRegistroValidaLosDatosYNoPermiteAdministrador() throws Exception {
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"no-es-correo\",\"clave\":\"123\",\"rol\":\"HUESPED\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Intruso\",\"correo\":\"intruso-" + UUID.randomUUID()
                                + "@prueba.com\",\"clave\":\"Clave2026*\",\"rol\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void laDocumentacionSwaggerEstaDisponible() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
