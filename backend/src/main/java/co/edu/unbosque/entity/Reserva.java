package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Reserva de un alojamiento hecha por un huésped.
 * Tabla: reserva
 */
@Entity
@Table(name = "reserva", indexes = {
        @Index(name = "idx_reserva_huesped", columnList = "huesped_id"),
        @Index(name = "idx_reserva_alojamiento", columnList = "alojamiento_id, estado")
})
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "huesped_id", nullable = false)
    private Long huespedId;

    @Column(name = "alojamiento_id", nullable = false)
    private Long alojamientoId;

    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "num_huespedes", nullable = false)
    private Integer numHuespedes;

    @Column(name = "noches", nullable = false)
    private Integer noches;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "tarifa_limpieza", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaLimpieza;

    @Column(name = "tarifa_servicio", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaServicio;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    /** HU-42: la publicación fue bloqueada y el administrador debe revisar esta reserva. */
    @Column(name = "requiere_revision")
    private Boolean requiereRevision;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public Reserva() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHuespedId() { return huespedId; }
    public void setHuespedId(Long huespedId) { this.huespedId = huespedId; }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public LocalDate getFechaLlegada() { return fechaLlegada; }
    public void setFechaLlegada(LocalDate fechaLlegada) { this.fechaLlegada = fechaLlegada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public Integer getNumHuespedes() { return numHuespedes; }
    public void setNumHuespedes(Integer numHuespedes) { this.numHuespedes = numHuespedes; }

    public Integer getNoches() { return noches; }
    public void setNoches(Integer noches) { this.noches = noches; }

    public BigDecimal getPrecioBase() { return precioBase; }
    public void setPrecioBase(BigDecimal precioBase) { this.precioBase = precioBase; }

    public BigDecimal getTarifaLimpieza() { return tarifaLimpieza; }
    public void setTarifaLimpieza(BigDecimal tarifaLimpieza) { this.tarifaLimpieza = tarifaLimpieza; }

    public BigDecimal getTarifaServicio() { return tarifaServicio; }
    public void setTarifaServicio(BigDecimal tarifaServicio) { this.tarifaServicio = tarifaServicio; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public Boolean getRequiereRevision() {
        return requiereRevision != null && requiereRevision;
    }

    public void setRequiereRevision(Boolean requiereRevision) {
        this.requiereRevision = requiereRevision;
    }

    /**
     * PATRÓN BUILDER: arma una reserva paso a paso y valida al final.
     * Toda reserva nueva nace con estado PENDIENTE_PAGO o EN_REVISION y fecha de creación actual.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Reserva reserva = new Reserva();

        public Builder huesped(Long huespedId) { reserva.huespedId = huespedId; return this; }
        public Builder alojamiento(Long alojamientoId) { reserva.alojamientoId = alojamientoId; return this; }
        public Builder fechas(LocalDate llegada, LocalDate salida) {
            reserva.fechaLlegada = llegada;
            reserva.fechaSalida = salida;
            return this;
        }
        public Builder numHuespedes(Integer numHuespedes) { reserva.numHuespedes = numHuespedes; return this; }
        public Builder noches(Integer noches) { reserva.noches = noches; return this; }
        public Builder precioBase(BigDecimal precioBase) { reserva.precioBase = precioBase; return this; }
        public Builder tarifaLimpieza(BigDecimal tarifaLimpieza) { reserva.tarifaLimpieza = tarifaLimpieza; return this; }
        public Builder tarifaServicio(BigDecimal tarifaServicio) { reserva.tarifaServicio = tarifaServicio; return this; }
        public Builder descuento(BigDecimal descuento) { reserva.descuento = descuento; return this; }
        public Builder total(BigDecimal total) { reserva.total = total; return this; }
        public Builder estado(String estado) { reserva.estado = estado; return this; }

        public Reserva build() {
            if (reserva.huespedId == null || reserva.alojamientoId == null) {
                throw new IllegalStateException("La reserva necesita huésped y alojamiento.");
            }
            if (reserva.fechaLlegada == null || reserva.fechaSalida == null
                    || !reserva.fechaSalida.isAfter(reserva.fechaLlegada)) {
                throw new IllegalStateException("Las fechas de la reserva no son válidas.");
            }
            if (reserva.total == null || reserva.estado == null) {
                throw new IllegalStateException("La reserva necesita total y estado.");
            }
            reserva.creadaEn = LocalDateTime.now();
            reserva.requiereRevision = false;
            return reserva;
        }
    }
}
