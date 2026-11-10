package co.edu.unbosque.utils;

/**
 * La cuenta existe y la clave es correcta, pero el correo aún no fue confirmado → 403
 * con el código CORREO_NO_VERIFICADO (ver GlobalExceptionHandler).
 */
public class CorreoNoVerificadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String correo;

    public CorreoNoVerificadoException(String message, String correo) {
        super(message);
        this.correo = correo;
    }

    public String getCorreo() {
        return correo;
    }
}
