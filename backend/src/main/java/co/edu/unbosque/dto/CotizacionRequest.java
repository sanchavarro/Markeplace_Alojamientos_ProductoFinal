package co.edu.unbosque.dto;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para calcular una cotización (HU-23).
 */
public class CotizacionRequest {

    @NotNull(message = "El alojamiento es obligatorio.")
    private Long alojamientoId;

    @NotNull(message = "La fecha de llegada es obligatoria.")
    private LocalDate fechaLlegada;

    @NotNull(message = "La fecha de salida es obligatoria.")
    private LocalDate fechaSalida;

    @NotNull(message = "El número de huéspedes es obligatorio.")
    @Min(value = 1, message = "Debe haber al menos un huésped.")
    private Integer numHuespedes;

    private List<Long> serviciosAdicionales;

    public CotizacionRequest() {
    }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public LocalDate getFechaLlegada() { return fechaLlegada; }
    public void setFechaLlegada(LocalDate fechaLlegada) { this.fechaLlegada = fechaLlegada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public Integer getNumHuespedes() { return numHuespedes; }
    public void setNumHuespedes(Integer numHuespedes) { this.numHuespedes = numHuespedes; }

    public List<Long> getServiciosAdicionales() { return serviciosAdicionales; }
    public void setServiciosAdicionales(List<Long> serviciosAdicionales) { this.serviciosAdicionales = serviciosAdicionales; }
}
