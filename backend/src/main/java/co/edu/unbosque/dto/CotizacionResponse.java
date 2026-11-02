package co.edu.unbosque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import co.edu.unbosque.entity.ServicioAdicional;

/**
 * Desglose del precio calculado en el servidor (HU-23, T12).
 */
public class CotizacionResponse {

    private Long alojamientoId;

    private LocalDate fechaLlegada;

    private LocalDate fechaSalida;

    private Integer numHuespedes;

    private Integer noches;

    private BigDecimal precioNoche;

    private BigDecimal precioBase;

    private BigDecimal tarifaLimpieza;

    private BigDecimal serviciosAdicionales;

    private BigDecimal tarifaServicio;

    private BigDecimal descuento;

    private BigDecimal total;

    private String moneda;

    private List<ServicioAdicional> detalleServicios;

    public CotizacionResponse() {
    }

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

    public BigDecimal getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(BigDecimal precioNoche) { this.precioNoche = precioNoche; }

    public BigDecimal getPrecioBase() { return precioBase; }
    public void setPrecioBase(BigDecimal precioBase) { this.precioBase = precioBase; }

    public BigDecimal getTarifaLimpieza() { return tarifaLimpieza; }
    public void setTarifaLimpieza(BigDecimal tarifaLimpieza) { this.tarifaLimpieza = tarifaLimpieza; }

    public BigDecimal getServiciosAdicionales() { return serviciosAdicionales; }
    public void setServiciosAdicionales(BigDecimal serviciosAdicionales) { this.serviciosAdicionales = serviciosAdicionales; }

    public BigDecimal getTarifaServicio() { return tarifaServicio; }
    public void setTarifaServicio(BigDecimal tarifaServicio) { this.tarifaServicio = tarifaServicio; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public List<ServicioAdicional> getDetalleServicios() { return detalleServicios; }
    public void setDetalleServicios(List<ServicioAdicional> detalleServicios) { this.detalleServicios = detalleServicios; }
}
