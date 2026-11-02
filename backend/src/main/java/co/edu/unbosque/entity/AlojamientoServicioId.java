package co.edu.unbosque.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Llave compuesta de alojamiento_servicio (alojamiento_id + servicio_id).
 */
public class AlojamientoServicioId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long alojamientoId;
    private Long servicioId;

    public AlojamientoServicioId() {
    }

    public AlojamientoServicioId(Long alojamientoId, Long servicioId) {
        this.alojamientoId = alojamientoId;
        this.servicioId = servicioId;
    }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long servicioId) { this.servicioId = servicioId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlojamientoServicioId otro)) return false;
        return Objects.equals(alojamientoId, otro.alojamientoId) && Objects.equals(servicioId, otro.servicioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alojamientoId, servicioId);
    }
}
