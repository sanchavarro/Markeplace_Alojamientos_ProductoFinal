package co.edu.unbosque.dto;

import java.math.BigDecimal;

/**
 * Publicación en listas y resultados de búsqueda.
 */
public class AlojamientoResumenDTO {

    private Long id;

    private String titulo;

    private String ciudad;

    private String tipo;

    private Integer capacidad;

    private BigDecimal precioNoche;

    private String modoReserva;

    private String estado;

    private String imagenPrincipal;

    private Double promedioCalificacion;

    private Long totalResenas;

    public AlojamientoResumenDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public BigDecimal getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(BigDecimal precioNoche) { this.precioNoche = precioNoche; }

    public String getModoReserva() { return modoReserva; }
    public void setModoReserva(String modoReserva) { this.modoReserva = modoReserva; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getImagenPrincipal() { return imagenPrincipal; }
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }

    public Double getPromedioCalificacion() { return promedioCalificacion; }
    public void setPromedioCalificacion(Double promedioCalificacion) { this.promedioCalificacion = promedioCalificacion; }

    public Long getTotalResenas() { return totalResenas; }
    public void setTotalResenas(Long totalResenas) { this.totalResenas = totalResenas; }
}
