package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.TarifaRequest;
import co.edu.unbosque.service.api.ConfiguracionTarifaServiceAPI;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * HU-24 · Configurar porcentajes de tarifas (administrador).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/tarifas")
public class TarifaRestController {

    @Autowired
    private ConfiguracionTarifaServiceAPI configuracionTarifaServiceAPI;

    // GET /tarifas
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(configuracionTarifaServiceAPI.getAll());
    }

    // PUT /tarifas/{clave}
    @PutMapping("/{clave}")
    public ResponseEntity<?> actualizar(@PathVariable String clave, @Valid @RequestBody TarifaRequest request,
                                        Authentication auth) {
        return ResponseEntity.ok(configuracionTarifaServiceAPI.actualizar(Utilidad.idUsuarioActual(auth), clave,
                request.getValor()));
    }
}
