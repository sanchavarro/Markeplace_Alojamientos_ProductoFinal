package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.persistence.*;

/**
 * Servicio adicional contratado en una reserva, con el precio cobrado en ese momento.
 * Tabla: reserva_servicio_adicional
 */
@Entity
@Table(name = "reserva_servicio_adicional")
@IdClass(ReservaServicioAdicionalId.class)
public class ReservaServicioAdicional implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Id
    @Column(name = "servicio_adicional_id", nullable = false)
    private Long servicioAdicionalId;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    public ReservaServicioAdicional() {
    }

    public ReservaServicioAdicional(Long reservaId, Long servicioAdicionalId, BigDecimal precio) {
        this.reservaId = reservaId;
        this.servicioAdicionalId = servicioAdicionalId;
        this.precio = precio;
    }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public Long getServicioAdicionalId() { return servicioAdicionalId; }
    public void setServicioAdicionalId(Long servicioAdicionalId) { this.servicioAdicionalId = servicioAdicionalId; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
