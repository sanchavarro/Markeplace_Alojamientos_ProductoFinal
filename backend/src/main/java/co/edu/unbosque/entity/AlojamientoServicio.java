package co.edu.unbosque.entity;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Relación muchos a muchos entre alojamiento y servicio (servicios incluidos).
 * Tabla: alojamiento_servicio
 */
@Entity
@Table(name = "alojamiento_servicio")
@IdClass(AlojamientoServicioId.class)
public class AlojamientoServicio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "alojamiento_id", nullable = false)
    private Long alojamientoId;

    @Id
    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    public AlojamientoServicio() {
    }

    public AlojamientoServicio(Long alojamientoId, Long servicioId) {
        this.alojamientoId = alojamientoId;
        this.servicioId = servicioId;
    }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long servicioId) { this.servicioId = servicioId; }
}
