package co.edu.unbosque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nueva clave usando el token de recuperación (HU-04).
 */
public class RestablecerClaveRequest {

    @NotBlank(message = "El token de recuperación es obligatorio.")
    private String token;

    @NotBlank(message = "La nueva clave es obligatoria.")
    @Size(min = 8, max = 64, message = "La clave debe tener entre 8 y 64 caracteres.")
    private String nuevaClave;

    public RestablecerClaveRequest() {
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNuevaClave() { return nuevaClave; }
    public void setNuevaClave(String nuevaClave) { this.nuevaClave = nuevaClave; }
}
