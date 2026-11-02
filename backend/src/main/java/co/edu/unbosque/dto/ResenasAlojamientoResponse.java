package co.edu.unbosque.dto;

import co.edu.unbosque.entity.Resena;

/**
 * Reseñas visibles de un alojamiento con su promedio (HU-40).
 */
public class ResenasAlojamientoResponse {

    private Long alojamientoId;

    private Double promedio;

    private Long total;

    private PaginaResponse<Resena> resenas;

    public ResenasAlojamientoResponse() {
    }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public Double getPromedio() { return promedio; }
    public void setPromedio(Double promedio) { this.promedio = promedio; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public PaginaResponse<Resena> getResenas() { return resenas; }
    public void setResenas(PaginaResponse<Resena> resenas) { this.resenas = resenas; }
}
