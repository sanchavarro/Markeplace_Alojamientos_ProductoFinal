package co.edu.unbosque.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un usuario (HU-01).
 */
public class RegistroRequest {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 120, message = "El nombre admite máximo 120 caracteres.")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo debe tener un formato válido.")
    @Size(max = 150, message = "El correo admite máximo 150 caracteres.")
    private String correo;

    @NotBlank(message = "La clave es obligatoria.")
    @Size(min = 8, max = 64, message = "La clave debe tener entre 8 y 64 caracteres.")
    private String clave;

    @NotBlank(message = "El rol es obligatorio (HUESPED o ANFITRION).")
    private String rol;

    /** Identificador del captcha entregado por GET /auth/captcha. */
    @Size(max = 64, message = "El identificador del captcha no es válido.")
    private String captchaId;

    /** Texto que el usuario leyó en la imagen del captcha. */
    @Size(max = 12, message = "La respuesta del captcha no es válida.")
    private String captchaRespuesta;

    public RegistroRequest() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getCaptchaId() { return captchaId; }
    public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }

    public String getCaptchaRespuesta() { return captchaRespuesta; }
    public void setCaptchaRespuesta(String captchaRespuesta) { this.captchaRespuesta = captchaRespuesta; }
}
