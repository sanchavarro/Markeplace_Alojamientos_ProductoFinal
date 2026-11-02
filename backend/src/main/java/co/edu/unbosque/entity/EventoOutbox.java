package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evento pendiente de notificar (ADR-07). Se guarda en la misma transacción del negocio.
 * Tabla: evento_outbox
 */
@Entity
@Table(name = "evento_outbox", indexes = {
        @Index(name = "idx_evento_outbox_estado", columnList = "estado, creado_en")
})
public class EventoOutbox implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "carga", nullable = false, columnDefinition = "jsonb")
    private String carga;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "intentos", nullable = false)
    private Integer intentos;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public EventoOutbox() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCarga() { return carga; }
    public void setCarga(String carga) { this.carga = carga; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
