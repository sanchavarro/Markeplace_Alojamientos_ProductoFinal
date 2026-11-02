package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.CancelacionRequest;
import co.edu.unbosque.service.api.CancelacionServiceAPI;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * Épica E08 · Cancelación (HU-32 a HU-35).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/reservas/{id}/cancelacion")
public class CancelacionRestController {

    @Autowired
    private CancelacionServiceAPI cancelacionServiceAPI;

    // GET /reservas/{id}/cancelacion/simulacion — HU-33 ver cuánto se devolvería
    @GetMapping("/simulacion")
    public ResponseEntity<?> simular(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(cancelacionServiceAPI.simular(Utilidad.idUsuarioActual(auth), Utilidad.rolActual(auth), id));
    }

    // POST /reservas/{id}/cancelacion — HU-32, HU-34, HU-35
    @PostMapping
    public ResponseEntity<?> cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) CancelacionRequest request,
                                      Authentication auth) {
        String motivo = request == null ? null : request.getMotivo();
        return ResponseEntity.ok(cancelacionServiceAPI.cancelar(Utilidad.idUsuarioActual(auth), Utilidad.rolActual(auth),
                id, motivo));
    }
}
