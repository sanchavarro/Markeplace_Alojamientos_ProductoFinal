package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** PAT-02 Estrategia: cálculo de la devolución según la política (HU-33). */
class EstrategiaCancelacionTest {

    private final BigDecimal total = new BigDecimal("200000.00");
    private final LocalDate hoy = LocalDate.of(2026, 11, 1);

    @Test
    void flexibleDevuelveTodoConDosDiasOMas() {
        EstrategiaCancelacion e = FabricaEstrategiaCancelacion.obtener(Constantes.POLITICA_FLEXIBLE);
        assertEquals(new BigDecimal("200000.00"), e.calcularDevolucion(total, hoy, hoy.plusDays(2)));
    }

    @Test
    void flexibleDevuelveLaMitadConMenosDeDosDias() {
        EstrategiaCancelacion e = FabricaEstrategiaCancelacion.obtener(Constantes.POLITICA_FLEXIBLE);
        assertEquals(new BigDecimal("100000.00"), e.calcularDevolucion(total, hoy, hoy.plusDays(1)));
    }

    @Test
    void estrictaDevuelveLaMitadConSieteDiasOMas() {
        EstrategiaCancelacion e = FabricaEstrategiaCancelacion.obtener(Constantes.POLITICA_ESTRICTA);
        assertEquals(new BigDecimal("100000.00"), e.calcularDevolucion(total, hoy, hoy.plusDays(7)));
    }

    @Test
    void estrictaNoDevuelveNadaConMenosDeSieteDias() {
        EstrategiaCancelacion e = FabricaEstrategiaCancelacion.obtener(Constantes.POLITICA_ESTRICTA);
        assertEquals(new BigDecimal("0.00"), e.calcularDevolucion(total, hoy, hoy.plusDays(6)));
    }
}
