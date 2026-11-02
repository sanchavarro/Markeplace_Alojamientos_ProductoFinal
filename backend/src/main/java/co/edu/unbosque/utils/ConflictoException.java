package co.edu.unbosque.utils;

/**
 * Conflicto con el estado actual, por ejemplo fechas no disponibles → 409 (ADR-02).
 */
public class ConflictoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String message) {
        super(message);
    }
}
