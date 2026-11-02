package co.edu.unbosque.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.service.api.ConfiguracionTarifaServiceAPI;
import co.edu.unbosque.service.api.PoliticaCancelacionServiceAPI;
import co.edu.unbosque.service.api.ServicioServiceAPI;

/**
 * Catálogos públicos: políticas de cancelación y servicios incluidos.
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/catalogo")
public class CatalogoRestController {

    @Autowired
    private PoliticaCancelacionServiceAPI politicaCancelacionServiceAPI;

    @Autowired
    private ServicioServiceAPI servicioServiceAPI;

    @Autowired
    private ConfiguracionTarifaServiceAPI configuracionTarifaServiceAPI;

    // GET /catalogo/politicas
    @GetMapping("/politicas")
    public ResponseEntity<?> politicas() {
        return ResponseEntity.ok(politicaCancelacionServiceAPI.getAll());
    }

    // GET /catalogo/servicios
    @GetMapping("/servicios")
    public ResponseEntity<?> servicios() {
        return ResponseEntity.ok(servicioServiceAPI.getAll());
    }

    // GET /catalogo/tarifas — HU-24 (parámetros vigentes)
    @GetMapping("/tarifas")
    public ResponseEntity<?> tarifas() {
        return ResponseEntity.ok(configuracionTarifaServiceAPI.getAll());
    }
}
