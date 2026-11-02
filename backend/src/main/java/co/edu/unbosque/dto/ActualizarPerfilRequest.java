package co.edu.unbosque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Cambios del perfil (HU-05). La clave solo cambia si se envían claveActual y nuevaClave.
 */
public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 120, message = "El nombre admite máximo 120 caracteres.")
    private String nombre;

    @Pattern(regexp = "^$|^[0-9+ ]{7,20}$", message = "El teléfono solo admite números, espacios y + (7 a 20 caracteres).")
    private String telefono;

    @Size(max = 500, message = "La URL de la foto admite máximo 500 caracteres.")
    private String fotoUrl;

    private String claveActual;

    @Size(min = 8, max = 64, message = "La clave debe tener entre 8 y 64 caracteres.")
    private String nuevaClave;

    public ActualizarPerfilRequest() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getClaveActual() { return claveActual; }
    public void setClaveActual(String claveActual) { this.claveActual = claveActual; }

    public String getNuevaClave() { return nuevaClave; }
    public void setNuevaClave(String nuevaClave) { this.nuevaClave = nuevaClave; }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}
