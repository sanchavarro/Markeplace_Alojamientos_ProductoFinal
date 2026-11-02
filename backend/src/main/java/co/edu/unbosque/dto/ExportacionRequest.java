package co.edu.unbosque.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de exportación en segundo plano (HU-47). Las fechas son opcionales (por defecto, el último mes).
 */
public class ExportacionRequest {

    @NotBlank(message = "El formato es obligatorio (csv, pdf o excel).")
    private String formato;

    private LocalDate desde;

    private LocalDate hasta;

    public ExportacionRequest() {
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }
}
