package co.edu.unbosque.utils;

/**
 * La pasarela no responde o el circuito está abierto → 503 (ADR-04).
 */
public class ServicioNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServicioNoDisponibleException(String message) {
        super(message);
    }
}
