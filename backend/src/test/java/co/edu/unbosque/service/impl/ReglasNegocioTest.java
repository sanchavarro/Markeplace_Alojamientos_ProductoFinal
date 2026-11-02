package co.edu.unbosque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.PeriodoDisponibilidad;
import co.edu.unbosque.entity.ReporteExportacion;
import co.edu.unbosque.repository.NotificacionRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.DisponibilidadServiceAPI;
import co.edu.unbosque.service.api.ReporteServiceAPI;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.DespachadorOutbox;

/**
 * Reglas de negocio que cruzan varios módulos: cuentas, publicaciones bloqueadas, calendario,
 * auditoría de solo inserción (ADR-06), outbox (ADR-07) y reportes en segundo plano (HU-47).
 */
class ReglasNegocioTest extends PruebaIntegracionBase {

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    @Autowired
    private UsuarioServiceAPI usuarioServiceAPI;

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    @Autowired
    private DisponibilidadServiceAPI disponibilidadServiceAPI;

    @Autowired
    private ReporteServiceAPI reporteServiceAPI;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private DespachadorOutbox despachadorOutbox;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** HU-06: no se puede desactivar la cuenta con reservas confirmadas pendientes (huésped y anfitrión). */
    @Test
    void noSeDesactivaLaCuentaConReservasPendientes() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 10), clave());

        assertThrows(ConflictoException.class, () -> usuarioServiceAPI.desactivarCuenta(huesped));
        assertThrows(ConflictoException.class, () -> usuarioServiceAPI.desactivarCuenta(alojamiento.getAnfitrionId()));
    }

    /** HU-42: al bloquear la publicación las reservas futuras se conservan y quedan en revisión manual. */
    @Test
    void bloquearPublicacionMarcaLasReservasParaRevision() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaResponse reserva = reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 10), clave());

        alojamientoServiceAPI.cambiarEstadoPorAdministrador(administrador().getId(), alojamiento.getId(),
                Constantes.ALOJAMIENTO_BLOQUEADA, "Incumple las reglas");

        var guardada = reservaRepository.findById(reserva.getId()).orElseThrow();
        assertEquals(Constantes.RESERVA_CONFIRMADA, guardada.getEstado());
        assertTrue(guardada.getRequiereRevision());
    }

    /** HU-16: un periodo con reservas no se puede borrar ni recortar dejando la reserva por fuera. */
    @Test
    void noSeBorraUnPeriodoConReservas() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 10), clave());
        PeriodoDisponibilidad disponible = periodoRepository.findByAlojamientoIdOrderByFechaInicioAsc(alojamiento.getId())
                .stream().filter(p -> Constantes.PERIODO_DISPONIBLE.equals(p.getEstado())).findFirst().orElseThrow();

        assertThrows(ConflictoException.class, () -> disponibilidadServiceAPI.eliminarPeriodo(
                alojamiento.getAnfitrionId(), alojamiento.getId(), disponible.getId()));
    }

    /** ADR-06: la tabla de auditoría rechaza UPDATE y DELETE (trigger de solo inserción). */
    @Test
    void laAuditoriaNoSePuedeEditarNiBorrar() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        reservaServiceAPI.reservar(crearUsuario(Constantes.ROL_HUESPED).getId(), solicitud(alojamiento.getId(), 10),
                clave());
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM evento_auditoria", Long.class);

        assertThrows(Exception.class,
                () -> jdbcTemplate.update("UPDATE evento_auditoria SET accion = 'X' WHERE id = ?", id));
        assertThrows(Exception.class, () -> jdbcTemplate.update("DELETE FROM evento_auditoria WHERE id = ?", id));
    }

    /** ADR-07: la reserva deja eventos en el outbox y el despachador crea las notificaciones. */
    @Test
    void elOutboxCreaLasNotificaciones() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 10), clave());

        // El despachador procesa 50 eventos por vuelta (los más viejos primero). Otras pruebas dejan muchos
        // eventos pendientes, así que se llama varias veces, como hace la tarea programada cada 10 s.
        for (int vuelta = 0; vuelta < 100 && notificacionRepository.countByUsuarioIdAndLeidaFalse(huesped) == 0; vuelta++) {
            despachadorOutbox.despachar();
        }

        assertTrue(notificacionRepository.countByUsuarioIdAndLeidaFalse(huesped) > 0);
        assertTrue(notificacionRepository.countByUsuarioIdAndLeidaFalse(alojamiento.getAnfitrionId()) > 0);
    }

    /** HU-47: el reporte se genera en segundo plano y queda LISTO para descargar en Excel. */
    @Test
    void elReporteSeGeneraEnSegundoPlano() throws InterruptedException {
        ReporteExportacion solicitud = reporteServiceAPI.solicitarExportacion(administrador().getId(), "excel",
                null, null);
        assertEquals("PENDIENTE", solicitud.getEstado());

        String estado = "PENDIENTE";
        for (int i = 0; i < 50 && List.of("PENDIENTE", "PROCESANDO").contains(estado); i++) {
            Thread.sleep(200);
            estado = reporteServiceAPI.obtenerExportacion(solicitud.getId()).getEstado();
        }
        assertEquals("LISTO", estado);
        ReporteExportacion lista = reporteServiceAPI.descargarExportacion(administrador().getId(), solicitud.getId());
        assertTrue(lista.getArchivo().length > 0);
        assertTrue(lista.getNombreArchivo().endsWith(".xlsx"));
    }
}
