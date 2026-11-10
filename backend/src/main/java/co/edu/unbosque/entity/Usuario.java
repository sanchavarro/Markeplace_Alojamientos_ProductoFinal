package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Usuario registrado (huésped, anfitrión o administrador).
 * Tabla: usuario
 */
@Entity
@Table(name = "usuario", indexes = {
        @Index(name = "idx_usuario_rol", columnList = "rol")
})
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @JsonIgnore
    @Column(name = "hash_contrasena", nullable = false, length = 255)
    private String hashContrasena;

    @Column(name = "rol", nullable = false, length = 15)
    private String rol;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    /**
     * Verificación del correo al registrarse. NULL se trata como verificado para las cuentas creadas
     * antes de esta funcionalidad (ddl-auto=update agrega la columna vacía).
     */
    @Column(name = "correo_verificado")
    private Boolean correoVerificado;

    @JsonIgnore
    @Column(name = "codigo_verificacion_hash", length = 255)
    private String codigoVerificacionHash;

    @JsonIgnore
    @Column(name = "codigo_verificacion_expira")
    private LocalDateTime codigoVerificacionExpira;

    @JsonIgnore
    @Column(name = "codigo_verificacion_enviado")
    private LocalDateTime codigoVerificacionEnviado;

    @JsonIgnore
    @Column(name = "intentos_verificacion")
    private Integer intentosVerificacion;

    public Usuario() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getHashContrasena() { return hashContrasena; }
    public void setHashContrasena(String hashContrasena) { this.hashContrasena = hashContrasena; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public Boolean getCorreoVerificado() { return correoVerificado; }
    public void setCorreoVerificado(Boolean correoVerificado) { this.correoVerificado = correoVerificado; }

    /** true salvo que la columna sea explícitamente FALSE (cuentas nuevas aún sin verificar). */
    @JsonIgnore
    public boolean isCorreoConfirmado() { return !Boolean.FALSE.equals(correoVerificado); }

    public String getCodigoVerificacionHash() { return codigoVerificacionHash; }
    public void setCodigoVerificacionHash(String codigoVerificacionHash) { this.codigoVerificacionHash = codigoVerificacionHash; }

    public LocalDateTime getCodigoVerificacionExpira() { return codigoVerificacionExpira; }
    public void setCodigoVerificacionExpira(LocalDateTime codigoVerificacionExpira) { this.codigoVerificacionExpira = codigoVerificacionExpira; }

    public LocalDateTime getCodigoVerificacionEnviado() { return codigoVerificacionEnviado; }
    public void setCodigoVerificacionEnviado(LocalDateTime codigoVerificacionEnviado) { this.codigoVerificacionEnviado = codigoVerificacionEnviado; }

    public Integer getIntentosVerificacion() { return intentosVerificacion; }
    public void setIntentosVerificacion(Integer intentosVerificacion) { this.intentosVerificacion = intentosVerificacion; }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}
