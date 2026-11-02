package co.edu.unbosque.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar una publicación (HU-07, HU-08, HU-11 a HU-14).
 */
public class AlojamientoRequest {

    @NotBlank(message = "El título es obligatorio.")
    @Size(min = 5, max = 120, message = "El título debe tener entre 5 y 120 caracteres.")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(min = 20, message = "La descripción debe tener al menos 20 caracteres.")
    private String descripcion;

    @Size(max = 2000, message = "Las reglas admiten máximo 2000 caracteres.")
    private String reglas;

    @NotBlank(message = "La ciudad es obligatoria.")
    @Size(max = 80, message = "La ciudad admite máximo 80 caracteres.")
    private String ciudad;

    @NotBlank(message = "La dirección es obligatoria.")
    @Size(max = 150, message = "La dirección admite máximo 150 caracteres.")
    private String direccion;

    @NotBlank(message = "El tipo es obligatorio (CASA, APARTAMENTO, FINCA, CABANA, HABITACION).")
    private String tipo;

    @NotNull(message = "La capacidad es obligatoria.")
    @Max(value = 50, message = "La capacidad máxima es 50 personas.")
    private Integer capacidad;

    @NotNull(message = "El precio por noche es obligatorio.")
    private BigDecimal precioNoche;

    @DecimalMin(value = "0", message = "La tarifa de limpieza no puede ser negativa.")
    private BigDecimal tarifaLimpieza;

    @NotBlank(message = "El modo de reserva es obligatorio (INMEDIATA o APROBACION).")
    private String modoReserva;

    @NotNull(message = "La política de cancelación es obligatoria.")
    private Long politicaId;

    private List<Long> servicios;

    public AlojamientoRequest() {
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public BigDecimal getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(BigDecimal precioNoche) { this.precioNoche = precioNoche; }

    public BigDecimal getTarifaLimpieza() { return tarifaLimpieza; }
    public void setTarifaLimpieza(BigDecimal tarifaLimpieza) { this.tarifaLimpieza = tarifaLimpieza; }

    public String getModoReserva() { return modoReserva; }
    public void setModoReserva(String modoReserva) { this.modoReserva = modoReserva; }

    public Long getPoliticaId() { return politicaId; }
    public void setPoliticaId(Long politicaId) { this.politicaId = politicaId; }

    public List<Long> getServicios() { return servicios; }
    public void setServicios(List<Long> servicios) { this.servicios = servicios; }

    public String getReglas() {
        return reglas;
    }

    public void setReglas(String reglas) {
        this.reglas = reglas;
    }
}
