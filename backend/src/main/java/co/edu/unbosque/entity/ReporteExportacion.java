package co.edu.unbosque.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Exportación de un reporte que se genera en segundo plano (HU-47, ADR-08 T2).
 * Estados: PENDIENTE → PROCESANDO → LISTO o ERROR. El archivo queda guardado para descargarlo.
 * Tabla: reporte_exportacion
 */
@Entity
@Table(name = "reporte_exportacion", indexes = {
        @Index(name = "idx_reporte_exportacion_admin", columnList = "admin_id, creado_en"),
        @Index(name = "idx_reporte_exportacion_estado", columnList = "estado")
})
public class ReporteExportacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "formato", nullable = false, length = 10)
    private String formato;

    @Column(name = "desde", nullable = false)
    private LocalDate desde;

    @Column(name = "hasta", nullable = false)
    private LocalDate hasta;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "nombre_archivo", length = 120)
    private String nombreArchivo;

    @Column(name = "registros")
    private Integer registros;

    @JsonIgnore
    @Column(name = "archivo", columnDefinition = "BYTEA")
    private byte[] archivo;

    @Column(name = "mensaje_error", length = 255)
    private String mensajeError;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "terminado_en")
    private LocalDateTime terminadoEn;

    public ReporteExportacion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public Integer getRegistros() {
        return registros;
    }

    public void setRegistros(Integer registros) {
        this.registros = registros;
    }

    public byte[] getArchivo() {
        return archivo;
    }

    public void setArchivo(byte[] archivo) {
        this.archivo = archivo;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getTerminadoEn() {
        return terminadoEn;
    }

    public void setTerminadoEn(LocalDateTime terminadoEn) {
        this.terminadoEn = terminadoEn;
    }
}
