package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** PAT-06 Estado: transiciones válidas e inválidas. */
class MaquinaEstadosTest {

    @Test
    void permiteConfirmarUnaReservaPendienteDePago() {
        assertDoesNotThrow(() -> MaquinaEstados.validarReserva(Constantes.RESERVA_PENDIENTE_PAGO,
                Constantes.RESERVA_CONFIRMADA));
    }

    @Test
    void noPermiteConfirmarUnaReservaCancelada() {
        assertThrows(ReglaNegocioException.class, () -> MaquinaEstados.validarReserva(Constantes.RESERVA_CANCELADA,
                Constantes.RESERVA_CONFIRMADA));
    }

    @Test
    void unaPublicacionEnBorradorNoPuedeActivarseSinRevision() {
        assertThrows(ReglaNegocioException.class, () -> MaquinaEstados.validarPublicacion(
                Constantes.ALOJAMIENTO_BORRADOR, Constantes.ALOJAMIENTO_ACTIVA));
    }
}
