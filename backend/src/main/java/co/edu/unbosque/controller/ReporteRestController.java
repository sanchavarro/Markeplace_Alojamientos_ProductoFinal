package co.edu.unbosque.controller;

import org.springframework.http.HttpStatus;
import co.edu.unbosque.dto.ExportacionRequest;
import co.edu.unbosque.entity.ReporteExportacion;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.service.api.ReporteServiceAPI;
import co.edu.unbosque.utils.Utilidad;

/**
 * Épicas E12 · Dashboard (HU-46) y E13 · Reportes (HU-47, HU-48). Solo rol ADMINISTRADOR.
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/reportes")
public class ReporteRestController {

    @Autowired
    private ReporteServiceAPI reporteServiceAPI;

    // GET /reportes/dashboard — HU-46
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(reporteServiceAPI.dashboard());
    }

    // GET /reportes/reservas?formato=csv|pdf|excel&desde=2026-11-01&hasta=2026-11-30 — HU-47 descarga inmediata, HU-48
    @GetMapping("/reservas")
    public ResponseEntity<?> exportar(@RequestParam(defaultValue = "csv") String formato,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                      Authentication auth) {
        byte[] archivo = reporteServiceAPI.exportarReservas(Utilidad.idUsuarioActual(auth), formato, desde, hasta);
        String nombre = "reservas_" + LocalDate.now() + "." + reporteServiceAPI.extension(normalizar(formato));
        return archivo(nombre, normalizar(formato), archivo);
    }

    // POST /reportes/exportaciones {"formato": "excel", "desde": "2026-11-01", "hasta": "2026-11-30"}
    // HU-47: se genera en segundo plano con menor prioridad; al terminar llega una notificación.
    @PostMapping("/exportaciones")
    public ResponseEntity<?> solicitar(@Valid @RequestBody ExportacionRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(reporteServiceAPI.solicitarExportacion(
                Utilidad.idUsuarioActual(auth), request.getFormato(), request.getDesde(), request.getHasta()));
    }

    // GET /reportes/exportaciones — mis exportaciones y su estado
    @GetMapping("/exportaciones")
    public ResponseEntity<?> misExportaciones(@RequestParam(required = false) Integer pagina,
                                              @RequestParam(required = false) Integer tamano, Authentication auth) {
        return ResponseEntity.ok(reporteServiceAPI.misExportaciones(Utilidad.idUsuarioActual(auth), pagina, tamano));
    }

    // GET /reportes/exportaciones/{id} — estado (PENDIENTE, PROCESANDO, LISTO, ERROR)
    @GetMapping("/exportaciones/{id}")
    public ResponseEntity<?> estado(@PathVariable Long id) {
        return ResponseEntity.ok(reporteServiceAPI.obtenerExportacion(id));
    }

    // GET /reportes/exportaciones/{id}/archivo — descarga cuando está LISTO (409 si aún no)
    @GetMapping("/exportaciones/{id}/archivo")
    public ResponseEntity<?> descargar(@PathVariable Long id, Authentication auth) {
        ReporteExportacion exportacion = reporteServiceAPI.descargarExportacion(Utilidad.idUsuarioActual(auth), id);
        return archivo(exportacion.getNombreArchivo(), exportacion.getFormato(), exportacion.getArchivo());
    }

    private ResponseEntity<byte[]> archivo(String nombre, String formato, byte[] datos) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(reporteServiceAPI.tipoContenido(formato)))
                .body(datos);
    }

    private String normalizar(String formato) {
        String f = formato == null ? "CSV" : formato.trim().toUpperCase();
        return "XLSX".equals(f) ? "EXCEL" : f;
    }
}
