package co.edu.unbosque.entity;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Imagen de una publicación.
 * Tabla: imagen
 */
@Entity
@Table(name = "imagen", indexes = {
        @Index(name = "idx_imagen_alojamiento", columnList = "alojamiento_id")
})
public class Imagen implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "alojamiento_id", nullable = false)
    private Long alojamientoId;

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    public Imagen() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlojamientoId() { return alojamientoId; }
    public void setAlojamientoId(Long alojamientoId) { this.alojamientoId = alojamientoId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
