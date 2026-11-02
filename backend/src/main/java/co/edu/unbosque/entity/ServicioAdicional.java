package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.persistence.*;

/**
 * Servicio adicional con costo que ofrece un alojamiento (HU-13).
 * Tabla: servicio_adicional
 */
@Entity
@Table(name = "servicio_adicional", indexes = {
        @Index(name = "idx_servicio_adicional_alojamiento", columnList = "alojamiento_id")
})
public class ServicioAdicional implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "alojamiento_id", nullable = false)
    private Long alojamientoId;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    public ServicioAdicional() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
