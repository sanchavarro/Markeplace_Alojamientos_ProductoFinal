package co.edu.unbosque.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de recuperación de clave (HU-04).
 */
public class RecuperarClaveRequest {

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo debe tener un formato válido.")
    private String correo;

    public RecuperarClaveRequest() {
    }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}
