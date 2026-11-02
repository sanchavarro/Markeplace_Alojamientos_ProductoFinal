package co.edu.unbosque.utils;

/**
 * El usuario no tiene permiso sobre el recurso → 403 (T10).
 */
public class AccesoDenegadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccesoDenegadoException(String message) {
        super(message);
    }
}
