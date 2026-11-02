package co.edu.unbosque.utils;

import java.math.BigDecimal;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

/**
 * Familia "Stripe" de la fábrica abstracta (modo pruebas, claves sk_test_...).
 * La clave de idempotencia se envía a Stripe (ADR-03), así un reintento nunca cobra dos veces.
 */
public class FabricaPagoStripe implements FabricaPasarelaPago {

    private final String apiKey;

    public FabricaPagoStripe(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("pagos.modo=stripe requiere la variable STRIPE_SECRET_KEY (sk_test_...).");
        }
        this.apiKey = apiKey;
    }

    @Override
    public ProcesadorCobro crearProcesadorCobro() {
        return new CobroStripe(apiKey);
    }

    @Override
    public ProcesadorReembolso crearProcesadorReembolso() {
        return new ReembolsoStripe(apiKey);
    }

    @Override
    public String nombre() {
        return "stripe";
    }

    private static long aCentavos(BigDecimal monto) {
        return monto.setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    static class CobroStripe implements ProcesadorCobro {

        private final String apiKey;

        CobroStripe(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ResultadoPago cobrar(BigDecimal monto, String metodoPago, String claveIdempotencia, String descripcion) {
            try {
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(aCentavos(monto))
                        .setCurrency("cop")
                        .setDescription(descripcion)
                        .setPaymentMethod(metodoPago == null || metodoPago.isBlank() ? "pm_card_visa" : metodoPago)
                        .setConfirm(true)
                        .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build())
                        .build();
                RequestOptions opciones = RequestOptions.builder()
                        .setApiKey(apiKey)
                        .setIdempotencyKey(claveIdempotencia)
                        .build();
                PaymentIntent intento = PaymentIntent.create(params, opciones);
                if ("succeeded".equals(intento.getStatus())) {
                    return new ResultadoPago(true, intento.getId(), "Pago aprobado por Stripe.");
                }
                throw new PagoRechazadoException("Stripe no aprobó el pago (estado: " + intento.getStatus() + ").");
            } catch (CardException e) {
                throw new PagoRechazadoException("La tarjeta fue rechazada: " + e.getMessage());
            } catch (StripeException e) {
                throw new IllegalStateException("Error de comunicación con Stripe: " + e.getMessage(), e);
            }
        }
    }

    static class ReembolsoStripe implements ProcesadorReembolso {

        private final String apiKey;

        ReembolsoStripe(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ResultadoPago reembolsar(String referenciaPago, BigDecimal monto, String claveIdempotencia) {
            try {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(referenciaPago)
                        .setAmount(aCentavos(monto))
                        .build();
                RequestOptions opciones = RequestOptions.builder()
                        .setApiKey(apiKey)
                        .setIdempotencyKey(claveIdempotencia)
                        .build();
                Refund reembolso = Refund.create(params, opciones);
                boolean ok = "succeeded".equals(reembolso.getStatus()) || "pending".equals(reembolso.getStatus());
                return new ResultadoPago(ok, reembolso.getId(), "Reembolso " + reembolso.getStatus() + " en Stripe.");
            } catch (StripeException e) {
                throw new IllegalStateException("Error de comunicación con Stripe: " + e.getMessage(), e);
            }
        }
    }
}
