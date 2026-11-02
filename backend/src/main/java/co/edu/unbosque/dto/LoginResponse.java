package co.edu.unbosque.dto;

/**
 * Respuesta del inicio de sesión con el token JWT.
 */
public class LoginResponse {

    private String token;

    private String tipo;

    private Long expiraEnSegundos;

    private UsuarioDTO usuario;

    public LoginResponse() {
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Long getExpiraEnSegundos() { return expiraEnSegundos; }
    public void setExpiraEnSegundos(Long expiraEnSegundos) { this.expiraEnSegundos = expiraEnSegundos; }

    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }
}
