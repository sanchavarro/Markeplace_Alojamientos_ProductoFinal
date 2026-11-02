package co.edu.unbosque.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import co.edu.unbosque.entity.Imagen;
import co.edu.unbosque.entity.PoliticaCancelacion;
import co.edu.unbosque.entity.Servicio;
import co.edu.unbosque.entity.ServicioAdicional;

/**
 * Detalle completo de una publicación (HU-15).
 */
public class AlojamientoDTO {

    private Long id;

    private Long anfitrionId;

    private String anfitrionNombre;

    private String titulo;

    private String descripcion;

    private String reglas;

    private String ciudad;

    private String direccion;

    private String tipo;

    private Integer capacidad;

    private BigDecimal precioNoche;

    private BigDecimal tarifaLimpieza;

    private String modoReserva;

    private String estado;

    private PoliticaCancelacion politica;

    private List<Imagen> imagenes;

    private List<ServicioAdicional> serviciosAdicionales;

    private List<Servicio> servicios;

    private Double promedioCalificacion;

    private Long totalResenas;

    private LocalDateTime creadoEn;

    public AlojamientoDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAnfitrionId() { return anfitrionId; }
    public void setAnfitrionId(Long anfitrionId) { this.anfitrionId = anfitrionId; }

    public String getAnfitrionNombre() { return anfitrionNombre; }
    public void setAnfitrionNombre(String anfitrionNombre) { this.anfitrionNombre = anfitrionNombre; }

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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public PoliticaCancelacion getPolitica() { return politica; }
    public void setPolitica(PoliticaCancelacion politica) { this.politica = politica; }

    public List<Imagen> getImagenes() { return imagenes; }
    public void setImagenes(List<Imagen> imagenes) { this.imagenes = imagenes; }

    public List<ServicioAdicional> getServiciosAdicionales() { return serviciosAdicionales; }
    public void setServiciosAdicionales(List<ServicioAdicional> serviciosAdicionales) { this.serviciosAdicionales = serviciosAdicionales; }

    public List<Servicio> getServicios() { return servicios; }
    public void setServicios(List<Servicio> servicios) { this.servicios = servicios; }

    public Double getPromedioCalificacion() { return promedioCalificacion; }
    public void setPromedioCalificacion(Double promedioCalificacion) { this.promedioCalificacion = promedioCalificacion; }

    public Long getTotalResenas() { return totalResenas; }
    public void setTotalResenas(Long totalResenas) { this.totalResenas = totalResenas; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getReglas() {
        return reglas;
    }

    public void setReglas(String reglas) {
        this.reglas = reglas;
    }
}
