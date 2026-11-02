package co.edu.unbosque.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Servicio adicional con costo (HU-13).
 */
public class ServicioAdicionalRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio.")
    @Size(max = 80, message = "El nombre admite máximo 80 caracteres.")
    private String nombre;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0", message = "El precio no puede ser negativo.")
    private BigDecimal precio;

    public ServicioAdicionalRequest() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
