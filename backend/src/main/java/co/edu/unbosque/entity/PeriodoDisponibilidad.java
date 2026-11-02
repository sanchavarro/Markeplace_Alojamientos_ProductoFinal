package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDate;
import jakarta.persistence.*;

/**
 * Periodo del calendario de un alojamiento (HU-16 a HU-18). RESERVADO tiene reserva_id.
 * Tabla: periodo_disponibilidad
 */
@Entity
@Table(name = "periodo_disponibilidad", indexes = {
        @Index(name = "idx_periodo_alojamiento_fechas", columnList = "alojamiento_id, fecha_inicio, fecha_fin")
})
public class PeriodoDisponibilidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "alojamiento_id", nullable = false)
    private Long alojamientoId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    public PeriodoDisponibilidad() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
