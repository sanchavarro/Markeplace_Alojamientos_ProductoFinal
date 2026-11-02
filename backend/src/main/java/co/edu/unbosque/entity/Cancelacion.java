package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Datos de la cancelación de una reserva (HU-34).
 * Tabla: cancelacion
 */
@Entity
@Table(name = "cancelacion")
public class Cancelacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reserva_id", nullable = false, unique = true)
    private Long reservaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "valor_devolucion", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDevolucion;

    @Column(name = "estado_final", nullable = false, length = 30)
    private String estadoFinal;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Cancelacion() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public BigDecimal getValorDevolucion() { return valorDevolucion; }
    public void setValorDevolucion(BigDecimal valorDevolucion) { this.valorDevolucion = valorDevolucion; }

    public String getEstadoFinal() { return estadoFinal; }
    public void setEstadoFinal(String estadoFinal) { this.estadoFinal = estadoFinal; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
