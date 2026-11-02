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

    // POST /auth/registro — HU-01
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroRequest request) {
        logger.info("Registro de usuario con rol " + request.getRol());
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
