package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Clave de idempotencia de una solicitud de reserva o pago (ADR-03). Se guarda 24 horas.
 * Tabla: clave_idempotencia
 */
@Entity
@Table(name = "clave_idempotencia", indexes = {
        @Index(name = "idx_clave_idempotencia_expira", columnList = "expira_en")
})
public class ClaveIdempotencia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "clave", nullable = false)
    private UUID clave;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "respuesta", columnDefinition = "jsonb")
    private String respuesta;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    public ClaveIdempotencia() {
    }

    public UUID getClave() { return clave; }
    public void setClave(UUID clave) { this.clave = clave; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }

    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }
}
