package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.ResenaRequest;
import co.edu.unbosque.service.api.ResenaServiceAPI;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * Épica E10 · Reseñas y calificaciones (HU-38, HU-39). La consulta está en GET /alojamientos/{id}/resenas.
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/resenas")
public class ResenaRestController {

    @Autowired
    private ResenaServiceAPI resenaServiceAPI;

    // POST /resenas — HU-38
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ResenaRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaServiceAPI.crear(Utilidad.idUsuarioActual(auth), request));
    }
}
