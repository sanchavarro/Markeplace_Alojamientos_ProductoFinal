package co.edu.unbosque.dto;

/**
 * Método de pago de prueba de Stripe (pm_card_visa, pm_card_chargeDeclined...).
 */
public class PagarRequest {

    private String metodoPago;

    public PagarRequest() {
    }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}
