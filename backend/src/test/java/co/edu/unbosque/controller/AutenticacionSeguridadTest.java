package co.edu.unbosque.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.JwtUtil;

/**
 * Pruebas de seguridad de autenticacion. RNF-07 / ESC-07 y RNF-17 / ESC-17.
 * Verifica bloqueo de cuenta, integridad del JWT, revocacion al cerrar
 * sesion y restriccion administrativa de una sesion previamente valida.
 *
 * Requiere PostgreSQL local y el perfil de pruebas (marketplace_test).
 * NUNCA ejecutar estas pruebas apuntando a marketplace_db: el perfil test
 * utiliza spring.jpa.hibernate.ddl-auto=create-drop.
 */
@AutoConfigureMockMvc
class AutenticacionSeguridadTest extends PruebaIntegracionBase {

    private static final String CLAVE_CORRECTA = "ClaveSegura2026*";
    private static final String CLAVE_INCORRECTA = "ClaveFalsa2026*";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /** Usuario de prueba individual para no bloquear las cuentas demo del proyecto. */
    private Usuario nuevoHuesped() {
        Usuario usuario = crearUsuario(Constantes.ROL_HUESPED);
        usuario.setHashContrasena(passwordEncoder.encode(CLAVE_CORRECTA));
        usuario.setCorreoVerificado(true);
        return usuarioRepository.save(usuario);
    }

    private String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }

    private String iniciarSesion(Usuario usuario) throws Exception {
        String respuesta = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("correo", usuario.getCorreo(), "clave", CLAVE_CORRECTA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(respuesta);
        return body.path("token").asText();
    }

    @Test
    void cincoIntentosFallidosBloqueanCuentaPorQuinceMinutos() throws Exception {
        Usuario usuario = nuevoHuesped();
        String solicitud = json(Map.of("correo", usuario.getCorreo(), "clave", CLAVE_INCORRECTA));

        for (int intento = 1; intento <= 4; intento++) {
            mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud))
                    .andExpect(status().isUnauthorized());
        }

        // Quinto intento: bloqueo temporal en vez de otro 401.
        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isLocked());

        Usuario bloqueado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertNotNull(bloqueado.getBloqueadoHasta());
        assertTrue(bloqueado.getBloqueadoHasta().isAfter(LocalDateTime.now().plusMinutes(14)),
                "El bloqueo debe durar aproximadamente 15 minutos");

        // Ni la clave correcta debe permitir iniciar sesion mientras dure el bloqueo.
        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("correo", usuario.getCorreo(), "clave", CLAVE_CORRECTA))))
                .andExpect(status().isLocked());
    }

    @Test
    void modificarFirmaDelJwtImpideAccesoAlPerfil() throws Exception {
        Usuario usuario = nuevoHuesped();
        String token = iniciarSesion(usuario);

        mockMvc.perform(get("/usuarios/perfil")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String[] partes = token.split("\\.");
        String firma = partes[2];
        char primerCaracterDistinto = firma.charAt(0) == 'A' ? 'B' : 'A';
        String tokenAlterado = partes[0] + "." + partes[1] + "."
                + primerCaracterDistinto + firma.substring(1);

        mockMvc.perform(get("/usuarios/perfil")
                    .header("Authorization", "Bearer " + tokenAlterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cerrarSesionRevocaJwtAunqueNoHayaExpirado() throws Exception {
        Usuario usuario = nuevoHuesped();
        String token = iniciarSesion(usuario);

        mockMvc.perform(get("/usuarios/perfil")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/usuarios/perfil")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restringirUsuarioInvalidaSesionQueYaEstabaAbierta() throws Exception {
        Usuario huesped = nuevoHuesped();
        String tokenHuesped = iniciarSesion(huesped);

        mockMvc.perform(get("/usuarios/perfil")
                    .header("Authorization", "Bearer " + tokenHuesped))
                .andExpect(status().isOk());

        // Admin precreado por DatosIniciales en el perfil de prueba.
        Usuario admin = administrador();
        String tokenAdmin = jwtUtil.generarTokenAcceso(admin.getId(), Constantes.ROL_ADMINISTRADOR);

        mockMvc.perform(patch("/admin/usuarios/{id}", huesped.getId())
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("accion", "RESTRINGIR", "motivo", "Prueba automatizada de seguridad"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(Constantes.USUARIO_RESTRINGIDO));

        mockMvc.perform(get("/usuarios/perfil")
                    .header("Authorization", "Bearer " + tokenHuesped))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("correo", huesped.getCorreo(), "clave", CLAVE_CORRECTA))))
                .andExpect(status().isLocked());
    }
}
