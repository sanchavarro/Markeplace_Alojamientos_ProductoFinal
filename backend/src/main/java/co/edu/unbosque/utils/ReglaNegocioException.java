package co.edu.unbosque.utils;

/**
 * Se incumple una regla de negocio → 422.
 */
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String message) {
        super(message);
    }
}
