package co.edu.unbosque.utils;

/**
 * Cuenta bloqueada temporalmente o restringida → 423 (ADR-05).
 */
public class CuentaBloqueadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CuentaBloqueadaException(String message) {
        super(message);
    }
}
