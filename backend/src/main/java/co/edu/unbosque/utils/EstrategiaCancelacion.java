package co.edu.unbosque.utils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PATRÓN ESTRATEGIA (PAT-02).
 * Cada política de cancelación calcula de forma distinta cuánto dinero se devuelve.
 */
public interface EstrategiaCancelacion {

    /** Porcentaje (0 a 100) que se devuelve según los días que faltan para la llegada. */
    int porcentajeDevolucion(LocalDate fechaCancelacion, LocalDate fechaLlegada);

    String descripcion();

    default BigDecimal calcularDevolucion(BigDecimal totalPagado, LocalDate fechaCancelacion, LocalDate fechaLlegada) {
        int porcentaje = porcentajeDevolucion(fechaCancelacion, fechaLlegada);
        return totalPagado.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}
