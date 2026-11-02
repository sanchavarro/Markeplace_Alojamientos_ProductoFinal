package co.edu.unbosque.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Llave compuesta de reserva_servicio_adicional (reserva_id + servicio_adicional_id).
 */
public class ReservaServicioAdicionalId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long reservaId;
    private Long servicioAdicionalId;

    public ReservaServicioAdicionalId() {
    }

    public ReservaServicioAdicionalId(Long reservaId, Long servicioAdicionalId) {
        this.reservaId = reservaId;
        this.servicioAdicionalId = servicioAdicionalId;
    }

    public Long getAlojamientoId() { return reservaId; }
    public void setAlojamientoId(Long reservaId) { this.reservaId = reservaId; }

    public Long getServicioId() { return servicioAdicionalId; }
    public void setServicioId(Long servicioAdicionalId) { this.servicioAdicionalId = servicioAdicionalId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReservaServicioAdicionalId otro)) return false;
        return Objects.equals(reservaId, otro.reservaId) && Objects.equals(servicioAdicionalId, otro.servicioAdicionalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservaId, servicioAdicionalId);
    }
}
