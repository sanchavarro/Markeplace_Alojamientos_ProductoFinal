package co.edu.unbosque.controller;

import co.edu.unbosque.dto.SaludResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.service.api.SaludServiceAPI;

@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/salud")
public class SaludRestController {

    @Autowired
    private SaludServiceAPI saludServiceAPI;

    // GET /salud — estado del sistema (público)
    @GetMapping
    public ResponseEntity<?> estado() {
        SaludResponse salud = saludServiceAPI.estado();
        // Si la base de datos no responde se devuelve 503 para que el monitoreo lo detecte.
        return ResponseEntity.status("caido".equals(salud.getEstado()) ? 503 : 200).body(salud);
    }

    // GET /salud/metricas — métricas de observabilidad (administrador)
    @GetMapping("/metricas")
    public ResponseEntity<?> metricas() {
        return ResponseEntity.ok(saludServiceAPI.metricas());
    }
}
