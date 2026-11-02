package co.edu.unbosque.dto;

import jakarta.validation.constraints.Size;

/**
 * Motivo opcional de una acción administrativa (queda en la auditoría).
 */
public class MotivoRequest {

    @Size(max = 300, message = "El motivo admite máximo 300 caracteres.")
    private String motivo;

    public MotivoRequest() {
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
