package co.edu.unbosque.dto;

import jakarta.validation.constraints.Size;

/**
 * Motivo de la cancelación (HU-32).
 */
public class CancelacionRequest {

    @Size(max = 255, message = "El motivo admite máximo 255 caracteres.")
    private String motivo;

    public CancelacionRequest() {
    }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
