package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Registro de auditoría, solo inserción y sin FK (ADR-06).
 * Tabla: evento_auditoria
 */
@Entity
@Table(name = "evento_auditoria", indexes = {
        @Index(name = "idx_evento_auditoria_fecha", columnList = "fecha"),
        @Index(name = "idx_evento_auditoria_entidad", columnList = "entidad, entidad_id")
})
public class EventoAuditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "entidad", nullable = false, length = 40)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle", columnDefinition = "jsonb")
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public EventoAuditoria() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
