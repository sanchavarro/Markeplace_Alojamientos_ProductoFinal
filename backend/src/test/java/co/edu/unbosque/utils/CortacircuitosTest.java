package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** PAT-05 Cortacircuitos y tiempo límite (ADR-04). */
class CortacircuitosTest {

    @Test
    void seAbreDespuesDeVariosFallosYRespondeSinLlamar() {
        Cortacircuitos c = new Cortacircuitos(2, 30, 1);
        for (int i = 0; i < 2; i++) {
            assertThrows(ServicioNoDisponibleException.class, () -> c.ejecutar(() -> {
                throw new IllegalStateException("pasarela caída");
            }));
        }
        assertEquals(Cortacircuitos.Estado.ABIERTO, c.getEstado());
        assertThrows(ServicioNoDisponibleException.class, () -> c.ejecutar(() -> "no debería ejecutarse"));
    }

    @Test
    void cortaLasLlamadasQueSuperanElTiempoLimite() {
        Cortacircuitos c = new Cortacircuitos(5, 30, 1);
        assertThrows(ServicioNoDisponibleException.class, () -> c.ejecutar(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "tarde";
        }));
    }

    @Test
    void unRechazoDeTarjetaNoCuentaComoFallo() {
        Cortacircuitos c = new Cortacircuitos(1, 30, 1);
        assertThrows(PagoRechazadoException.class, () -> c.ejecutar(() -> {
            throw new PagoRechazadoException("rechazada");
        }));
        assertEquals(Cortacircuitos.Estado.CERRADO, c.getEstado());
    }
}
