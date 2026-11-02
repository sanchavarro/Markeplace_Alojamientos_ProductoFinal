package co.edu.unbosque.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reseña de una estadía completada (HU-38).
 */
public class ResenaRequest {

    @NotNull(message = "La reserva es obligatoria.")
    private Long reservaId;

    @NotNull(message = "La calificación es obligatoria.")
    @Min(value = 1, message = "La calificación mínima es 1.")
    @Max(value = 5, message = "La calificación máxima es 5.")
    private Integer calificacion;

    @Size(max = 1000, message = "El comentario admite máximo 1000 caracteres.")
    private String comentario;

    public ResenaRequest() {
    }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public Integer getCalificacion() { return calificacion; }
    public void setCalificacion(Integer calificacion) { this.calificacion = calificacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
