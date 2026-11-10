package co.edu.unbosque.controller;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.LoginRequest;
import co.edu.unbosque.dto.RecuperarClaveRequest;
import co.edu.unbosque.dto.RegistroRequest;
import co.edu.unbosque.dto.RestablecerClaveRequest;
import co.edu.unbosque.dto.VerificarCorreoRequest;
import co.edu.unbosque.utils.ServicioCaptcha;
import co.edu.unbosque.service.api.AuthServiceAPI;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * Épica E01 · Registro e inicio de sesión (HU-01 a HU-04, HU-06).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthRestController {

    private static final Logger logger = Logger.getLogger(AuthRestController.class.getName());

    @Autowired
    private AuthServiceAPI authServiceAPI;

    @Autowired
    private ServicioCaptcha servicioCaptcha;

    // GET /auth/captcha — captcha del registro (imagen SVG + identificador de un solo uso)
    @GetMapping("/captcha")
    public ResponseEntity<?> captcha() {
        java.util.Map<String, Object> cuerpo = servicioCaptcha.generar();
        cuerpo.put("habilitado", servicioCaptcha.estaHabilitado());
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(cuerpo);
    }

    // POST /auth/verificar-correo — confirma el correo con el código y devuelve la sesión
    @PostMapping("/verificar-correo")
    public ResponseEntity<?> verificarCorreo(@Valid @RequestBody VerificarCorreoRequest request) {
        return ResponseEntity.ok(authServiceAPI.verificarCorreo(request));
    }

    // POST /auth/reenviar-verificacion — nuevo código de confirmación
    @PostMapping("/reenviar-verificacion")
    public ResponseEntity<?> reenviarVerificacion(@Valid @RequestBody RecuperarClaveRequest request) {
        return ResponseEntity.ok(authServiceAPI.reenviarVerificacion(request.getCorreo()));
    }

    // POST /auth/registro — HU-01
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroRequest request) {
        // No se registran datos del cuerpo de la solicitud en el log (evita inyección en los logs).
        logger.info("Solicitud de registro de usuario");
        return ResponseEntity.status(HttpStatus.CREATED).body(authServiceAPI.registrar(request));
    }

    // POST /auth/login — HU-02
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authServiceAPI.login(request));
    }

    // POST /auth/logout — HU-02
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String autorizacion) {
        authServiceAPI.logout(autorizacion.replace("Bearer ", ""));
        return ResponseEntity.ok(Utilidad.mensaje("Sesión cerrada."));
    }

    // POST /auth/reactivar — HU-06
    @PostMapping("/reactivar")
    public ResponseEntity<?> reactivar(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authServiceAPI.reactivar(request));
    }

    // POST /auth/recuperar — HU-04 (simulada)
    @PostMapping("/recuperar")
    public ResponseEntity<?> recuperar(@Valid @RequestBody RecuperarClaveRequest request) {
        return ResponseEntity.ok(authServiceAPI.solicitarRecuperacion(request.getCorreo()));
    }

    // POST /auth/restablecer — HU-04
    @PostMapping("/restablecer")
    public ResponseEntity<?> restablecer(@Valid @RequestBody RestablecerClaveRequest request) {
        authServiceAPI.restablecerClave(request);
        return ResponseEntity.ok(Utilidad.mensaje("La clave fue actualizada. Ya puedes iniciar sesión."));
    }
}
