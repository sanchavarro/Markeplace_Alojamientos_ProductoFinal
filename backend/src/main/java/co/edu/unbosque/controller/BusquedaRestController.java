package co.edu.unbosque.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.service.api.BusquedaServiceAPI;

/**
 * Épica E05 · Búsqueda (HU-19 a HU-22). Pública y paginada (ADR-08).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/busqueda")
public class BusquedaRestController {

    @Autowired
    private BusquedaServiceAPI busquedaServiceAPI;

    // GET /busqueda?ciudad=Cartagena&llegada=2026-12-20&salida=2026-12-24&capacidad=2&tipo=CASA&precioMin=100000&precioMax=500000&servicios=1,3
    @GetMapping
    public ResponseEntity<?> buscar(@RequestParam(required = false) String ciudad,
                                    @RequestParam(required = false) String tipo,
                                    @RequestParam(required = false) Integer capacidad,
                                    @RequestParam(required = false) BigDecimal precioMin,
                                    @RequestParam(required = false) BigDecimal precioMax,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate llegada,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
                                    @RequestParam(required = false) List<Long> servicios,
                                    @RequestParam(required = false) Integer pagina,
                                    @RequestParam(required = false) Integer tamano) {
        return ResponseEntity.ok(busquedaServiceAPI.buscar(ciudad, tipo, capacidad, precioMin, precioMax, llegada,
                salida, servicios, pagina, tamano));
    }

    // GET /busqueda/comparar?ids=1,2,3 — HU-22
    @GetMapping("/comparar")
    public ResponseEntity<?> comparar(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(busquedaServiceAPI.comparar(ids));
    }
}
