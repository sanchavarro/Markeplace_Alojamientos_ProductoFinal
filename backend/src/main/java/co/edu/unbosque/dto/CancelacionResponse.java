package co.edu.unbosque.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado o simulación de una cancelación (HU-33, HU-34).
 */
public class CancelacionResponse {

    private Long reservaId;

    private String estadoReserva;

    private String politica;

    private String reglaPolitica;

    private Integer porcentajeDevolucion;

    private BigDecimal totalPagado;

    private BigDecimal valorDevolucion;

    private String estadoFinal;

    private String referenciaReembolso;

    private LocalDateTime fecha;

    public CancelacionResponse() {
    }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public String getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(String estadoReserva) { this.estadoReserva = estadoReserva; }

    public String getPolitica() { return politica; }
    public void setPolitica(String politica) { this.politica = politica; }

    public String getReglaPolitica() { return reglaPolitica; }
    public void setReglaPolitica(String reglaPolitica) { this.reglaPolitica = reglaPolitica; }

    public Integer getPorcentajeDevolucion() { return porcentajeDevolucion; }
    public void setPorcentajeDevolucion(Integer porcentajeDevolucion) { this.porcentajeDevolucion = porcentajeDevolucion; }

    public BigDecimal getTotalPagado() { return totalPagado; }
    public void setTotalPagado(BigDecimal totalPagado) { this.totalPagado = totalPagado; }

    public BigDecimal getValorDevolucion() { return valorDevolucion; }
    public void setValorDevolucion(BigDecimal valorDevolucion) { this.valorDevolucion = valorDevolucion; }

    public String getEstadoFinal() { return estadoFinal; }
    public void setEstadoFinal(String estadoFinal) { this.estadoFinal = estadoFinal; }

    public String getReferenciaReembolso() { return referenciaReembolso; }
    public void setReferenciaReembolso(String referenciaReembolso) { this.referenciaReembolso = referenciaReembolso; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
