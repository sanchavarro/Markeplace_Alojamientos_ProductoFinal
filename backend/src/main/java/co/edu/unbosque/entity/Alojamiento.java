package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Publicación de un alojamiento hecha por un anfitrión.
 * Tabla: alojamiento
 */
@Entity
@Table(name = "alojamiento", indexes = {
        @Index(name = "idx_alojamiento_ciudad_estado", columnList = "ciudad, estado"),
        @Index(name = "idx_alojamiento_anfitrion", columnList = "anfitrion_id")
})
public class Alojamiento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "anfitrion_id", nullable = false)
    private Long anfitrionId;

    @Column(name = "politica_id")
    private Long politicaId;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "reglas", columnDefinition = "TEXT")
    private String reglas;

    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad;

    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "precio_noche", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNoche;

    @Column(name = "tarifa_limpieza", precision = 12, scale = 2)
    private BigDecimal tarifaLimpieza;

    @Column(name = "modo_reserva", nullable = false, length = 12)
    private String modoReserva;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Alojamiento() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAnfitrionId() { return anfitrionId; }
    public void setAnfitrionId(Long anfitrionId) { this.anfitrionId = anfitrionId; }

    public Long getPoliticaId() { return politicaId; }
    public void setPoliticaId(Long politicaId) { this.politicaId = politicaId; }

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

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getReglas() {
        return reglas;
    }

    public void setReglas(String reglas) {
        this.reglas = reglas;
    }
}
