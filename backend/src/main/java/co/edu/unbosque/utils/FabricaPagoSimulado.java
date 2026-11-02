package co.edu.unbosque.utils;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Familia "simulada" de la fábrica abstracta. No necesita claves y sirve para las pruebas con Postman.
 * Usa los mismos nombres de método de pago de prueba que Stripe:
 *  - pm_card_visa (o vacío)       → aprobado
 *  - pm_card_chargeDeclined       → rechazado (402)
 *  - pm_simular_caida             → la pasarela falla (cuenta para el cortacircuitos, 503)
 *  - pm_simular_lento             → tarda 12 s (supera el tiempo límite de 10 s, 503)
 */
public class FabricaPagoSimulado implements FabricaPasarelaPago {

    @Override
    public ProcesadorCobro crearProcesadorCobro() {
        return new CobroSimulado();
    }

    @Override
    public ProcesadorReembolso crearProcesadorReembolso() {
        return new ReembolsoSimulado();
    }

    @Override
    public String nombre() {
        return "simulado";
    }

    static class CobroSimulado implements ProcesadorCobro {

        @Override
        public ResultadoPago cobrar(BigDecimal monto, String metodoPago, String claveIdempotencia, String descripcion) {
            String metodo = metodoPago == null || metodoPago.isBlank() ? "pm_card_visa" : metodoPago;
            switch (metodo) {
                case "pm_simular_caida":
                    throw new IllegalStateException("Simulación: la pasarela no responde.");
                case "pm_simular_lento":
                    try {
                        Thread.sleep(12000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new ResultadoPago(true, "sim_pi_" + UUID.randomUUID(), "Aprobado tarde (simulación).");
                default:
                    if (metodo.toLowerCase().contains("declined")) {
                        throw new PagoRechazadoException("La tarjeta fue rechazada por el banco (simulación).");
                    }
                    return new ResultadoPago(true, "sim_pi_" + UUID.randomUUID(), "Pago aprobado (simulación).");
            }
        }
    }

    static class ReembolsoSimulado implements ProcesadorReembolso {

        @Override
        public ResultadoPago reembolsar(String referenciaPago, BigDecimal monto, String claveIdempotencia) {
            return new ResultadoPago(true, "sim_re_" + UUID.randomUUID(), "Reembolso aprobado (simulación).");
        }
    }
}
