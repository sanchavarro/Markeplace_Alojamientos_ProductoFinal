package co.edu.unbosque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.dto.CancelacionResponse;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.Cancelacion;
import co.edu.unbosque.repository.CancelacionRepository;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.service.api.CancelacionServiceAPI;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.utils.AccesoDenegadoException;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;

/**
 * HU-32 a HU-35: cancelación, política de devolución, registro de la cancelación y liberación de fechas.
 */
class CancelacionServiceImplTest extends PruebaIntegracionBase {

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    @Autowired
    private CancelacionServiceAPI cancelacionServiceAPI;

    @Autowired
    private CancelacionRepository cancelacionRepository;

    @Autowired
    private PagoRepository pagoRepository;

    /** HU-33 FLEXIBLE con 10 días de anticipación → 100 %; HU-34 queda registrada; HU-35 fechas libres. */
    @Test
    void debeCancelarDevolverTodoYLiberarLasFechas() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaResponse reserva = reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 10), clave());

        CancelacionResponse cancelacion = cancelacionServiceAPI.cancelar(huesped, Constantes.ROL_HUESPED,
                reserva.getId(), "Cambio de planes");

        assertEquals(Constantes.RESERVA_CANCELADA, cancelacion.getEstadoReserva());
        assertEquals(100, (int) cancelacion.getPorcentajeDevolucion());
        assertEquals(0, reserva.getTotal().compareTo(cancelacion.getValorDevolucion()));
        Cancelacion registro = cancelacionRepository.findByReservaId(reserva.getId()).orElseThrow();
        assertEquals(huesped, registro.getUsuarioId());
        assertEquals("Cambio de planes", registro.getMotivo());
        assertEquals("CANCELADA_REEMBOLSO_TOTAL", registro.getEstadoFinal());
        assertEquals(Constantes.PAGO_DEVUELTO,
                pagoRepository.findByReservaIdOrderByCreadoEnDesc(reserva.getId()).get(0).getEstado());

        // HU-35: otro huésped puede reservar las mismas fechas.
        Long otro = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaResponse nueva = reservaServiceAPI.reservar(otro, solicitud(alojamiento.getId(), 10), clave());
        assertEquals(Constantes.RESERVA_CONFIRMADA, nueva.getEstado());
    }

    /** HU-33 ESTRICTA con menos de 7 días → 0 %. */
    @Test
    void politicaEstrictaConPocaAnticipacionNoDevuelve() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_ESTRICTA);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaResponse reserva = reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 3), clave());

        CancelacionResponse simulacion = cancelacionServiceAPI.simular(huesped, Constantes.ROL_HUESPED, reserva.getId());

        assertEquals(0, (int) simulacion.getPorcentajeDevolucion());
        assertEquals(0, BigDecimal.ZERO.compareTo(simulacion.getValorDevolucion()));
    }

    /** HU-32: solo el huésped dueño cancela (403) y no se cancela dos veces (409). */
    @Test
    void soloElDuenoCancelaYNoDosVeces() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        Long intruso = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaResponse reserva = reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 10), clave());

        assertThrows(AccesoDenegadoException.class,
                () -> cancelacionServiceAPI.cancelar(intruso, Constantes.ROL_HUESPED, reserva.getId(), null));
        cancelacionServiceAPI.cancelar(huesped, Constantes.ROL_HUESPED, reserva.getId(), null);
        assertThrows(ConflictoException.class,
                () -> cancelacionServiceAPI.cancelar(huesped, Constantes.ROL_HUESPED, reserva.getId(), null));
    }

    /** HU-42: si cancela el administrador (publicación bloqueada) se devuelve el 100 % aunque la política sea estricta. */
    @Test
    void elAdministradorDevuelveTodo() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_ESTRICTA);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaResponse reserva = reservaServiceAPI.reservar(huesped, solicitud(alojamiento.getId(), 3), clave());

        CancelacionResponse cancelacion = cancelacionServiceAPI.cancelar(administrador().getId(),
                Constantes.ROL_ADMINISTRADOR, reserva.getId(), "Publicación bloqueada");

        assertEquals(100, (int) cancelacion.getPorcentajeDevolucion());
        assertTrue(cancelacion.getValorDevolucion().compareTo(BigDecimal.ZERO) > 0);
    }
}
