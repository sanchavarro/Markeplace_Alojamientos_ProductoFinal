package co.edu.unbosque.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Pruebas de autorizacion por rol y propiedad de recursos.
 * RNF-08 / ASR-06 / ESC-08: 403 al intentar modificar recursos ajenos,
 * sin alterar la base de datos. Tambien verifica un acceso autorizado
 * (administrador puede editar) y la privacidad de borradores (404).
 *
 * Usa el perfil test con la base marketplace_test (ddl-auto=create-drop).
 * NO ejecutar apuntando a la base principal marketplace_db.
 */
@AutoConfigureMockMvc
class AutorizacionApiTest extends PruebaIntegracionBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtil.generarTokenAcceso(usuario.getId(), usuario.getRol());
    }

    private String json(Object objeto) throws Exception {
        return objectMapper.writeValueAsString(objeto);
    }

    private String datosEdicion(Alojamiento alojamiento, String titulo) throws Exception {
        AlojamientoRequest request = new AlojamientoRequest();
        request.setTitulo(titulo);
        request.setDescripcion("Descripcion valida para verificar el control de acceso al alojamiento.");
        request.setCiudad("Cartagena");
        request.setDireccion("Calle 1 # 2-3");
        request.setTipo("CASA");
        request.setCapacidad(4);
        request.setPrecioNoche(new BigDecimal("210000.00"));
        request.setTarifaLimpieza(new BigDecimal("50000.00"));
        request.setModoReserva(Constantes.MODO_INMEDIATA);
        request.setPoliticaId(alojamiento.getPoliticaId());
        return json(request);
    }

    @Test
    void huespedNoPuedeCrearPublicaciones() throws Exception {
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);
        Alojamiento ajeno = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        long cantidadAntes = alojamientoRepository.count();

        mockMvc.perform(post("/alojamientos")
                    .header("Authorization", token(huesped))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(datosEdicion(ajeno, "Publicacion ilegal del huesped")))
                .andExpect(status().isForbidden());

        assertEquals(cantidadAntes, alojamientoRepository.count(),
                "Un huesped no debe poder crear alojamientos");
    }

    @Test
    void anfitrionNoPuedeConsultarAuditoriaAdministrativa() throws Exception {
        Usuario anfitrion = crearUsuario(Constantes.ROL_ANFITRION);

        mockMvc.perform(get("/admin/auditoria")
                    .header("Authorization", token(anfitrion)))
                .andExpect(status().isForbidden());
    }

    @Test
    void huespedNoPuedeConsultarPublicacionesPrivadasDeAnfitrion() throws Exception {
        Usuario huesped = crearUsuario(Constantes.ROL_HUESPED);

        mockMvc.perform(get("/alojamientos/mios")
                    .header("Authorization", token(huesped)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anfitrionNoPuedeEditarAlojamientoDeOtroAnfitrion() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Usuario otroAnfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        String tituloOriginal = alojamiento.getTitulo();

        mockMvc.perform(put("/alojamientos/{id}", alojamiento.getId())
                    .header("Authorization", token(otroAnfitrion))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(datosEdicion(alojamiento, "Titulo que NO se debe guardar")))
                .andExpect(status().isForbidden());

        assertEquals(tituloOriginal,
                alojamientoRepository.findById(alojamiento.getId()).orElseThrow().getTitulo(),
                "La edicion rechazada no debe modificar la publicacion");
    }

    @Test
    void anfitrionNoPuedePausarAlojamientoAjeno() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Usuario otroAnfitrion = crearUsuario(Constantes.ROL_ANFITRION);

        mockMvc.perform(patch("/alojamientos/{id}/estado", alojamiento.getId())
                    .header("Authorization", token(otroAnfitrion))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("accion", "PAUSAR"))))
                .andExpect(status().isForbidden());

        assertEquals(Constantes.ALOJAMIENTO_ACTIVA,
                alojamientoRepository.findById(alojamiento.getId()).orElseThrow().getEstado(),
                "El estado de la publicacion ajena debe conservarse");
    }

    @Test
    void anfitrionNoPuedeModificarDisponibilidadDeAlojamientoAjeno() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Usuario otroAnfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        long periodosAntes = periodoRepository.count();

        mockMvc.perform(post("/alojamientos/{id}/periodos", alojamiento.getId())
                    .header("Authorization", token(otroAnfitrion))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(
                            "fechaInicio", LocalDate.now().plusDays(5).toString(),
                            "fechaFin", LocalDate.now().plusDays(8).toString(),
                            "estado", "BLOQUEADO"))))
                .andExpect(status().isForbidden());

        assertEquals(periodosAntes, periodoRepository.count(),
                "No deben guardarse periodos sobre el calendario ajeno");
    }

    @Test
    void borradorSoloPuedeVerloSuPropietarioOAdministrador() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        alojamiento.setEstado(Constantes.ALOJAMIENTO_BORRADOR);
        alojamientoRepository.save(alojamiento);

        Usuario propietario = usuarioRepository.findById(alojamiento.getAnfitrionId()).orElseThrow();
        Usuario otroAnfitrion = crearUsuario(Constantes.ROL_ANFITRION);
        Usuario admin = administrador();

        mockMvc.perform(get("/alojamientos/{id}", alojamiento.getId())
                    .header("Authorization", token(otroAnfitrion)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/alojamientos/{id}", alojamiento.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/alojamientos/{id}", alojamiento.getId())
                    .header("Authorization", token(propietario)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/alojamientos/{id}", alojamiento.getId())
                    .header("Authorization", token(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void administradorSiPuedeEditarPublicacionDeOtroAnfitrion() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        String tituloNuevo = "Publicacion editada por administrador";

        mockMvc.perform(put("/alojamientos/{id}", alojamiento.getId())
                    .header("Authorization", token(administrador()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(datosEdicion(alojamiento, tituloNuevo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value(tituloNuevo));

        assertEquals(tituloNuevo,
                alojamientoRepository.findById(alojamiento.getId()).orElseThrow().getTitulo(),
                "Un administrador debe poder editar cualquier publicacion");
    }
}
