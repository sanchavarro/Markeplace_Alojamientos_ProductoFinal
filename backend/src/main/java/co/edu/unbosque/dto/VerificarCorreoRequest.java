package co.edu.unbosque.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Confirmación del correo con el código de 6 dígitos enviado al registrarse.
 */
public class VerificarCorreoRequest {

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo debe tener un formato válido.")
    private String correo;

    @NotBlank(message = "El código es obligatorio.")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos.")
    private String codigo;

    public VerificarCorreoRequest() {
    }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
