package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.ActualizarPerfilRequest;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * Épica E02 · Perfil (HU-05, HU-06).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/usuarios")
public class UsuarioRestController {

    @Autowired
    private UsuarioServiceAPI usuarioServiceAPI;

    // GET /usuarios/perfil — HU-05
    @GetMapping("/perfil")
    public ResponseEntity<?> perfil(Authentication auth) {
        return ResponseEntity.ok(usuarioServiceAPI.perfil(Utilidad.idUsuarioActual(auth)));
    }

    // PUT /usuarios/perfil — HU-05
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizar(@Valid @RequestBody ActualizarPerfilRequest request, Authentication auth) {
        return ResponseEntity.ok(usuarioServiceAPI.actualizarPerfil(Utilidad.idUsuarioActual(auth), request));
    }

    // PATCH /usuarios/perfil/desactivar — HU-06
    @PatchMapping("/perfil/desactivar")
    public ResponseEntity<?> desactivar(Authentication auth) {
        return ResponseEntity.ok(usuarioServiceAPI.desactivarCuenta(Utilidad.idUsuarioActual(auth)));
    }
}
