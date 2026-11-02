package co.edu.unbosque.utils;

/**
 * La pasarela rechazó el pago → 402 (ADR-04).
 */
public class PagoRechazadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PagoRechazadoException(String message) {
        super(message);
    }
}
