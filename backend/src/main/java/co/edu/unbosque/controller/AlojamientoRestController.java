package co.edu.unbosque.controller;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.AlojamientoRequest;
import co.edu.unbosque.dto.CambioEstadoRequest;
import co.edu.unbosque.dto.ImagenRequest;
import co.edu.unbosque.dto.PeriodoRequest;
import co.edu.unbosque.dto.ServicioAdicionalRequest;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.DisponibilidadServiceAPI;
import co.edu.unbosque.service.api.ResenaServiceAPI;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * Épicas E03 · Alojamientos y publicaciones (HU-07 a HU-15) y E04 · Disponibilidad (HU-16, HU-17).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/alojamientos")
public class AlojamientoRestController {

    private static final Logger logger = Logger.getLogger(AlojamientoRestController.class.getName());

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    @Autowired
    private DisponibilidadServiceAPI disponibilidadServiceAPI;

    @Autowired
    private ResenaServiceAPI resenaServiceAPI;

    // POST /alojamientos — HU-07 crear publicación (queda en BORRADOR)
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody AlojamientoRequest request, Authentication auth) {
        Long anfitrionId = Utilidad.idUsuarioActual(auth);
        logger.info("Creando publicación del anfitrión " + anfitrionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(alojamientoServiceAPI.crear(anfitrionId, request));
    }

    // PUT /alojamientos/{id} — HU-08 editar publicación
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @Valid @RequestBody AlojamientoRequest request,
                                    Authentication auth) {
        return ResponseEntity.ok(alojamientoServiceAPI.editar(Utilidad.idUsuarioActual(auth), Utilidad.rolActual(auth),
                id, request));
    }

    // GET /alojamientos/{id} — HU-15 detalle (público si está ACTIVA)
    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(alojamientoServiceAPI.detalle(id, Utilidad.idUsuarioOpcional(auth),
                Utilidad.rolActual(auth)));
    }

    // GET /alojamientos/mios — publicaciones del anfitrión
    @GetMapping("/mios")
    public ResponseEntity<?> misAlojamientos(@RequestParam(required = false) Integer pagina,
                                             @RequestParam(required = false) Integer tamano, Authentication auth) {
        return ResponseEntity.ok(alojamientoServiceAPI.misAlojamientos(Utilidad.idUsuarioActual(auth), pagina, tamano));
    }

    // PATCH /alojamientos/{id}/estado — HU-09 y HU-10 (ENVIAR_REVISION, PAUSAR, ACTIVAR)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request,
                                           Authentication auth) {
        return ResponseEntity.ok(alojamientoServiceAPI.cambiarEstadoPorAnfitrion(Utilidad.idUsuarioActual(auth), id,
                request.getAccion()));
    }

    // POST /alojamientos/{id}/imagenes
    @PostMapping("/{id}/imagenes")
    public ResponseEntity<?> agregarImagen(@PathVariable Long id, @Valid @RequestBody ImagenRequest request,
                                           Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alojamientoServiceAPI.agregarImagen(Utilidad.idUsuarioActual(auth), id, request));
    }

    // POST /alojamientos/{id}/imagenes/archivo (multipart: archivo, orden) — HU-07 subir foto
    @PostMapping(value = "/{id}/imagenes/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirImagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo,
                                         @RequestParam(required = false) Integer orden, Authentication auth)
            throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(alojamientoServiceAPI.subirImagen(
                Utilidad.idUsuarioActual(auth), id, archivo.getBytes(), archivo.getContentType(), orden));
    }

    // DELETE /alojamientos/{id}/imagenes/{imagenId}
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<?> eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId, Authentication auth) {
        alojamientoServiceAPI.eliminarImagen(Utilidad.idUsuarioActual(auth), id, imagenId);
        return ResponseEntity.ok(Utilidad.mensaje("Imagen eliminada."));
    }

    // POST /alojamientos/{id}/servicios-adicionales — HU-13
    @PostMapping("/{id}/servicios-adicionales")
    public ResponseEntity<?> agregarServicioAdicional(@PathVariable Long id,
                                                      @Valid @RequestBody ServicioAdicionalRequest request,
                                                      Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alojamientoServiceAPI.agregarServicioAdicional(Utilidad.idUsuarioActual(auth), id, request));
    }

    // DELETE /alojamientos/{id}/servicios-adicionales/{servicioId}
    @DeleteMapping("/{id}/servicios-adicionales/{servicioId}")
    public ResponseEntity<?> eliminarServicioAdicional(@PathVariable Long id, @PathVariable Long servicioId,
                                                       Authentication auth) {
        alojamientoServiceAPI.eliminarServicioAdicional(Utilidad.idUsuarioActual(auth), id, servicioId);
        return ResponseEntity.ok(Utilidad.mensaje("Servicio adicional eliminado."));
    }

    // POST /alojamientos/{id}/periodos — HU-16 abrir (DISPONIBLE) o cerrar (BLOQUEADO) fechas
    @PostMapping("/{id}/periodos")
    public ResponseEntity<?> crearPeriodo(@PathVariable Long id, @Valid @RequestBody PeriodoRequest request,
                                          Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadServiceAPI.crearPeriodo(Utilidad.idUsuarioActual(auth), id, request));
    }

    // PUT /alojamientos/{id}/periodos/{periodoId} — HU-16 editar periodo
    @PutMapping("/{id}/periodos/{periodoId}")
    public ResponseEntity<?> editarPeriodo(@PathVariable Long id, @PathVariable Long periodoId,
                                           @Valid @RequestBody PeriodoRequest request, Authentication auth) {
        return ResponseEntity.ok(disponibilidadServiceAPI.editarPeriodo(Utilidad.idUsuarioActual(auth), id, periodoId,
                request));
    }

    // DELETE /alojamientos/{id}/periodos/{periodoId} — HU-16
    @DeleteMapping("/{id}/periodos/{periodoId}")
    public ResponseEntity<?> eliminarPeriodo(@PathVariable Long id, @PathVariable Long periodoId, Authentication auth) {
        disponibilidadServiceAPI.eliminarPeriodo(Utilidad.idUsuarioActual(auth), id, periodoId);
        return ResponseEntity.ok(Utilidad.mensaje("Periodo eliminado."));
    }

    // GET /alojamientos/{id}/calendario?desde=2026-12-01&hasta=2026-12-31 — HU-17 (público)
    @GetMapping("/{id}/calendario")
    public ResponseEntity<?> calendario(@PathVariable Long id,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(disponibilidadServiceAPI.calendario(id, desde, hasta));
    }

    // GET /alojamientos/{id}/resenas — HU-40 (público)
    @GetMapping("/{id}/resenas")
    public ResponseEntity<?> resenas(@PathVariable Long id, @RequestParam(required = false) Integer pagina,
                                     @RequestParam(required = false) Integer tamano) {
        return ResponseEntity.ok(resenaServiceAPI.listar(id, pagina, tamano));
    }
}
