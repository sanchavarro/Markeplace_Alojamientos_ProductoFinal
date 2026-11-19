package co.edu.unbosque.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.utils.Constantes;

/**
 * Verifica que la busqueda de alojamientos trata los filtros como datos,
 * sin alterar la consulta a PostgreSQL (RNF-09 / ESC-09).
 * Utiliza la base marketplace_test por heredar de PruebaIntegracionBase.
 */
@AutoConfigureMockMvc
class InyeccionSqlTest extends PruebaIntegracionBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void filtroCiudadNoDebeEjecutarInyeccionSql() throws Exception {
        // Control positivo: existe una publicacion ACTIVA con esta ciudad unica.
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        String ciudad = "PruebaSQL" + UUID.randomUUID().toString().replace("-", "");
        alojamiento.setCiudad(ciudad);
        alojamientoRepository.save(alojamiento);

        mockMvc.perform(get("/busqueda").param("ciudad", ciudad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));

        // Intento de alterar el WHERE mediante el parametro ciudad.
        mockMvc.perform(get("/busqueda").param("ciudad", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));

        mockMvc.perform(get("/busqueda").param("ciudad", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtroTipoNoDebeEjecutarInyeccionSql() throws Exception {
        crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);

        mockMvc.perform(get("/busqueda").param("tipo", "CASA' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }
}
