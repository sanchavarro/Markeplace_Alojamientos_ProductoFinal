package co.edu.unbosque.dto;

/**
 * Contenido JSON de un evento del outbox (ADR-07).
 */
public class CargaNotificacion {

    private Long usuarioId;

    private String titulo;

    private String mensaje;

    /** Enlace opcional que solo va en el correo (por ejemplo, el de recuperación de clave). */
    private String enlace;

    public CargaNotificacion() {
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }
}
