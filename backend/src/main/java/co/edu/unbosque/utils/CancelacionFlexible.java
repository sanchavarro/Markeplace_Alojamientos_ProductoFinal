package co.edu.unbosque.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Política FLEXIBLE: 100 % si se cancela con 2 días o más de anticipación; 50 % si es con menos.
 */
public class CancelacionFlexible implements EstrategiaCancelacion {

    @Override
    public int porcentajeDevolucion(LocalDate fechaCancelacion, LocalDate fechaLlegada) {
        long dias = ChronoUnit.DAYS.between(fechaCancelacion, fechaLlegada);
        return dias >= 2 ? 100 : 50;
    }

    @Override
    public String descripcion() {
        return "Flexible: devolución del 100 % con 2 o más días de anticipación; 50 % con menos.";
    }
}
