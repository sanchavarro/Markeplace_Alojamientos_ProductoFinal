package co.edu.unbosque.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.dto.LoginRequest;
import co.edu.unbosque.dto.LoginResponse;
import co.edu.unbosque.dto.RegistroRequest;
import co.edu.unbosque.dto.VerificarCorreoRequest;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.service.api.AuthServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.CorreoNoVerificadoException;
import co.edu.unbosque.utils.CredencialesInvalidasException;
import co.edu.unbosque.utils.ReglaNegocioException;

/**
 * Registro seguro (HU-01, HU-02): con la verificación de correo activa, la cuenta no inicia sesión
 * hasta confirmar el código de 6 dígitos. El captcha se prueba aparte (ServicioCaptchaTest).
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "seguridad.verificacion.correo.habilitada=true")
class VerificacionCorreoTest extends PruebaIntegracionBase {

    private static final String CLAVE = "ClaveSegura2026";

    @Autowired
    private AuthServiceAPI authServiceAPI;

    @Autowired
    private MockMvc mockMvc;

    private String registrar(String correo) {
        RegistroRequest r = new RegistroRequest();
        r.setNombre("Prueba Verificación");
        r.setCorreo(correo);
        r.setClave(CLAVE);
        r.setRol(Constantes.ROL_HUESPED);
        Map<String, Object> respuesta = authServiceAPI.registrar(r);
        assertEquals(Boolean.TRUE, respuesta.get("verificacionRequerida"));
        assertNotNull(respuesta.get("usuario"));
        return (String) respuesta.get("codigoVerificacionSimulado");
    }

    private LoginRequest login(String correo) {
        LoginRequest l = new LoginRequest();
        l.setCorreo(correo);
        l.setClave(CLAVE);
        return l;
    }

    private VerificarCorreoRequest verificacion(String correo, String codigo) {
        VerificarCorreoRequest v = new VerificarCorreoRequest();
        v.setCorreo(correo);
        v.setCodigo(codigo);
        return v;
    }

    private String nuevoCorreo() {
        return "verifica-" + UUID.randomUUID() + "@prueba.com";
    }

    @Test
    void laCuentaSoloIniciaSesionDespuesDeConfirmarElCorreo() throws Exception {
        String correo = nuevoCorreo();
        String codigo = registrar(correo);
        assertNotNull(codigo);
        assertEquals(6, codigo.length());

        assertThrows(CorreoNoVerificadoException.class, () -> authServiceAPI.login(login(correo)));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"clave\":\"" + CLAVE + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"));

        String incorrecto = "000000".equals(codigo) ? "111111" : "000000";
        assertThrows(CredencialesInvalidasException.class, () -> authServiceAPI.verificarCorreo(verificacion(correo, incorrecto)));
        // Reenviar antes de un minuto no está permitido.
        assertThrows(ReglaNegocioException.class, () -> authServiceAPI.reenviarVerificacion(correo));

        LoginResponse sesion = authServiceAPI.verificarCorreo(verificacion(correo, codigo));
        assertNotNull(sesion.getToken());
        assertNotNull(authServiceAPI.login(login(correo)).getToken());

        assertThrows(ReglaNegocioException.class, () -> authServiceAPI.verificarCorreo(verificacion(correo, codigo)));
        assertFalse(authServiceAPI.reenviarVerificacion(correo).containsKey("codigoVerificacionSimulado"));
    }

    @Test
    void elCodigoVencidoOConDemasiadosIntentosSeRechaza() {
        String correo = nuevoCorreo();
        String codigo = registrar(correo);
        Usuario u = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();

        u.setIntentosVerificacion(5);
        usuarioRepository.save(u);
        assertThrows(CredencialesInvalidasException.class, () -> authServiceAPI.verificarCorreo(verificacion(correo, codigo)));

        u.setIntentosVerificacion(0);
        u.setCodigoVerificacionExpira(LocalDateTime.now().minusMinutes(1));
        u.setCodigoVerificacionEnviado(LocalDateTime.now().minusMinutes(5));
        usuarioRepository.save(u);
        assertThrows(CredencialesInvalidasException.class, () -> authServiceAPI.verificarCorreo(verificacion(correo, codigo)));

        // Pasado el minuto de espera sí se puede pedir un código nuevo, y ese sí funciona.
        String nuevo = (String) authServiceAPI.reenviarVerificacion(correo).get("codigoVerificacionSimulado");
        assertNotNull(nuevo);
        assertNotNull(authServiceAPI.verificarCorreo(verificacion(correo, nuevo)).getToken());
    }

    @Test
    void noRevelaSiUnCorreoExiste() {
        String inexistente = nuevoCorreo();
        assertTrue(authServiceAPI.reenviarVerificacion(inexistente).containsKey("mensaje"));
        assertThrows(CredencialesInvalidasException.class, () -> authServiceAPI.verificarCorreo(verificacion(inexistente, "123456")));
    }

    @Test
    void losEndpointsDeCaptchaYVerificacionSonPublicos() throws Exception {
        mockMvc.perform(get("/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").exists())
                .andExpect(jsonPath("$.imagen").exists());
        mockMvc.perform(post("/auth/reenviar-verificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + nuevoCorreo() + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/verificar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + nuevoCorreo() + "\",\"codigo\":\"12\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/verificar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + nuevoCorreo() + "\",\"codigo\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }
}
