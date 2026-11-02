package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;

/**
 * Pago (o intento de pago) de una reserva.
 * Tabla: pago
 */
@Entity
@Table(name = "pago", indexes = {
        @Index(name = "idx_pago_reserva", columnList = "reserva_id")
})
public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "referencia_stripe", length = 80)
    private String referenciaStripe;

    @Column(name = "clave_stripe")
    private UUID claveStripe;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Pago() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getReferenciaStripe() { return referenciaStripe; }
    public void setReferenciaStripe(String referenciaStripe) { this.referenciaStripe = referenciaStripe; }

    public UUID getClaveStripe() { return claveStripe; }
    public void setClaveStripe(UUID claveStripe) { this.claveStripe = claveStripe; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
