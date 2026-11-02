package co.edu.unbosque.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Parámetro global de tarifas, por ejemplo TARIFA_SERVICIO_PORCENTAJE (HU-24).
 * Tabla: configuracion_tarifa
 */
@Entity
@Table(name = "configuracion_tarifa")
public class ConfiguracionTarifa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "clave", nullable = false, unique = true, length = 40)
    private String clave;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDateTime vigenteDesde;

    public ConfiguracionTarifa() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDateTime getVigenteDesde() { return vigenteDesde; }
    public void setVigenteDesde(LocalDateTime vigenteDesde) { this.vigenteDesde = vigenteDesde; }
}
