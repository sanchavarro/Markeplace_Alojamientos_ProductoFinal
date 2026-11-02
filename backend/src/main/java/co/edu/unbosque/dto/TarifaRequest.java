package co.edu.unbosque.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo valor de un parámetro de tarifas (HU-24).
 */
public class TarifaRequest {

    @NotNull(message = "El valor es obligatorio.")
    @DecimalMin(value = "0", message = "El valor no puede ser negativo.")
    @DecimalMax(value = "100", message = "El valor no puede ser mayor a 100.")
    private BigDecimal valor;

    public TarifaRequest() {
    }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
