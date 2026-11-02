package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Reseña y calificación de una estadía completada (HU-38 a HU-41).
 * Tabla: resena
 */
@Entity
@Table(name = "resena", indexes = {
        @Index(name = "idx_resena_alojamiento", columnList = "alojamiento_id, visible")
})
public class Resena implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reserva_id", nullable = false, unique = true)
    private Long reservaId;

    @Column(name = "huesped_id", nullable = false)
    private Long huespedId;

    @Column(name = "alojamiento_id", nullable = false)
    private Long alojamientoId;

    @Column(name = "calificacion", nullable = false)
    private Integer calificacion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "visible", nullable = false)
    private Boolean visible;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public Resena() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public Long getHuespedId() { return huespedId; }
    public void setHuespedId(Long huespedId) { this.huespedId = huespedId; }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public Integer getCalificacion() { return calificacion; }
    public void setCalificacion(Integer calificacion) { this.calificacion = calificacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
