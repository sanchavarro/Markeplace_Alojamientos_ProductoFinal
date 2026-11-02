package co.edu.unbosque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resultado de una reserva con su pago.
 */
public class ReservaResponse {

    private Long id;

    private Long alojamientoId;

    private String tituloAlojamiento;

    private Long huespedId;

    private String huespedNombre;

    private LocalDate fechaLlegada;

    private LocalDate fechaSalida;

    private Integer numHuespedes;

    private Integer noches;

    private BigDecimal precioBase;

    private BigDecimal tarifaLimpieza;

    private BigDecimal tarifaServicio;

    private BigDecimal descuento;

    private BigDecimal total;

    private String estado;

    /** HU-42: la publicación fue bloqueada y la reserva espera revisión del administrador. */
    private Boolean requiereRevision;

    private String estadoPago;

    private String referenciaPago;

    private String mensaje;

    private LocalDateTime creadaEn;

    public ReservaResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public String getTituloAlojamiento() { return tituloAlojamiento; }
    public void setTituloAlojamiento(String tituloAlojamiento) { this.tituloAlojamiento = tituloAlojamiento; }

    public Long getHuespedId() { return huespedId; }
    public void setHuespedId(Long huespedId) { this.huespedId = huespedId; }

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

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public String getHuespedNombre() {
        return huespedNombre;
    }

    public void setHuespedNombre(String huespedNombre) {
        this.huespedNombre = huespedNombre;
    }

    public Boolean getRequiereRevision() {
        return requiereRevision;
    }

    public void setRequiereRevision(Boolean requiereRevision) {
        this.requiereRevision = requiereRevision;
    }
}
