package co.edu.unbosque.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.service.api.NotificacionServiceAPI;
import co.edu.unbosque.utils.Utilidad;

/**
 * Épica E09 · Notificaciones (HU-36, HU-37).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/notificaciones")
public class NotificacionRestController {

    @Autowired
    private NotificacionServiceAPI notificacionServiceAPI;

    // GET /notificaciones — HU-37
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Integer pagina,
                                    @RequestParam(required = false) Integer tamano, Authentication auth) {
        return ResponseEntity.ok(notificacionServiceAPI.listar(Utilidad.idUsuarioActual(auth), pagina, tamano));
    }

    // GET /notificaciones/no-leidas
    @GetMapping("/no-leidas")
    public ResponseEntity<?> noLeidas(Authentication auth) {
        return ResponseEntity.ok(Map.of("noLeidas", notificacionServiceAPI.contarNoLeidas(Utilidad.idUsuarioActual(auth))));
    }

    // PATCH /notificaciones/{id}/leida — HU-37
    @PatchMapping("/{id}/leida")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(notificacionServiceAPI.marcarLeida(Utilidad.idUsuarioActual(auth), id));
    }

    // PATCH /notificaciones/leidas — marcar todas
    @PatchMapping("/leidas")
    public ResponseEntity<?> marcarTodas(Authentication auth) {
        int total = notificacionServiceAPI.marcarTodasLeidas(Utilidad.idUsuarioActual(auth));
        return ResponseEntity.ok(Map.of("marcadas", total));
    }
}
