package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.CotizacionRequest;
import co.edu.unbosque.service.api.CotizacionServiceAPI;
import jakarta.validation.Valid;

/**
 * Épica E06 · Cotización (HU-23 a HU-25).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/cotizaciones")
public class CotizacionRestController {

    @Autowired
    private CotizacionServiceAPI cotizacionServiceAPI;

    // POST /cotizaciones — HU-23
    @PostMapping
    public ResponseEntity<?> cotizar(@Valid @RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(cotizacionServiceAPI.cotizar(request));
    }
}
