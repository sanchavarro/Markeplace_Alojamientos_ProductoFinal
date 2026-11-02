package co.edu.unbosque.utils;

/**
 * Correo o clave incorrectos, o token inválido → 401 (ADR-05).
 */
public class CredencialesInvalidasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
