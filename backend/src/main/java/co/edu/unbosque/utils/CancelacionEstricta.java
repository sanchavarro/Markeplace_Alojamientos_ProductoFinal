package co.edu.unbosque.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Política ESTRICTA: 50 % si se cancela con 7 días o más de anticipación; 0 % si es con menos.
 */
public class CancelacionEstricta implements EstrategiaCancelacion {

    @Override
    public int porcentajeDevolucion(LocalDate fechaCancelacion, LocalDate fechaLlegada) {
        long dias = ChronoUnit.DAYS.between(fechaCancelacion, fechaLlegada);
        return dias >= 7 ? 50 : 0;
    }

    @Override
    public String descripcion() {
        return "Estricta: devolución del 50 % con 7 o más días de anticipación; 0 % con menos.";
    }
}
