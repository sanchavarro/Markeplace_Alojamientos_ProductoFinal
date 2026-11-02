package co.edu.unbosque.controller;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.dto.PagarRequest;
import co.edu.unbosque.dto.ReservaRequest;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.Utilidad;
import jakarta.validation.Valid;

/**
 * Épica E07 · Reserva (HU-26 a HU-31).
 * El encabezado Idempotency-Key (UUID) es obligatorio para crear o pagar (ADR-03).
 */
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/reservas")
public class ReservaRestController {

    private static final Logger logger = Logger.getLogger(ReservaRestController.class.getName());

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    // POST /reservas — HU-26 reserva inmediata (o solicitud si el alojamiento es con aprobación)
    @PostMapping
    public ResponseEntity<?> reservar(@Valid @RequestBody ReservaRequest request,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String clave,
                                      Authentication auth) {
        Long huespedId = Utilidad.idUsuarioActual(auth);
        logger.info("Solicitud de reserva del huésped " + huespedId + " para el alojamiento " + request.getAlojamientoId());
        return responder(reservaServiceAPI.reservar(huespedId, request, clave));
    }

    // POST /reservas/{id}/pagar — HU-31 pagar una reserva aprobada por el anfitrión
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Long id, @RequestBody(required = false) PagarRequest request,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String clave,
                                   Authentication auth) {
        String metodo = request == null ? null : request.getMetodoPago();
        return responder(reservaServiceAPI.pagar(Utilidad.idUsuarioActual(auth), id, metodo, clave));
    }

    // PATCH /reservas/{id}/aprobar — HU-14 el anfitrión aprueba una solicitud
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservaServiceAPI.aprobar(Utilidad.idUsuarioActual(auth), id));
    }

    // PATCH /reservas/{id}/rechazar — HU-14 el anfitrión rechaza una solicitud
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservaServiceAPI.rechazar(Utilidad.idUsuarioActual(auth), id));
    }

    // GET /reservas/mias — HU-29 historial del huésped
    @GetMapping("/mias")
    public ResponseEntity<?> mias(@RequestParam(required = false) Integer pagina,
                                  @RequestParam(required = false) Integer tamano, Authentication auth) {
        return ResponseEntity.ok(reservaServiceAPI.historialHuesped(Utilidad.idUsuarioActual(auth), pagina, tamano));
    }

    // GET /reservas/anfitrion — HU-30 reservas de mis alojamientos
    @GetMapping("/anfitrion")
    public ResponseEntity<?> anfitrion(@RequestParam(required = false) Long alojamientoId,
                                       @RequestParam(required = false) Integer pagina,
                                       @RequestParam(required = false) Integer tamano, Authentication auth) {
        return ResponseEntity.ok(reservaServiceAPI.reservasAnfitrion(Utilidad.idUsuarioActual(auth), alojamientoId,
                pagina, tamano));
    }

    // GET /reservas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservaServiceAPI.detalle(Utilidad.idUsuarioActual(auth), Utilidad.rolActual(auth), id));
    }

    /**
     * Código HTTP según el resultado:
     * 201 confirmada · 202 enviada al anfitrión · 402 pago rechazado · 503 pasarela no disponible.
     */
    private ResponseEntity<?> responder(ReservaResponse respuesta) {
        HttpStatus estado = HttpStatus.CREATED;
        if (Constantes.RESERVA_EN_REVISION.equals(respuesta.getEstado())) {
            estado = HttpStatus.ACCEPTED;
        } else if (Constantes.PAGO_NO_DISPONIBLE.equals(respuesta.getEstadoPago())) {
            estado = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (Constantes.RESERVA_PAGO_FALLIDO.equals(respuesta.getEstado())) {
            estado = HttpStatus.PAYMENT_REQUIRED;
        }
        return ResponseEntity.status(estado).body(respuesta);
    }
}
