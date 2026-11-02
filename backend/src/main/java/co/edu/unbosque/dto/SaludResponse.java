package co.edu.unbosque.dto;

import java.time.LocalDateTime;

/**
 * Estado del sistema (T8 Monitor).
 */
public class SaludResponse {

    private String estado;

    private String aplicacion;

    private String baseDatos;

    private LocalDateTime hora;

    private String pasarelaPagos;

    private String cortacircuitos;

    public SaludResponse() {
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getAplicacion() { return aplicacion; }
    public void setAplicacion(String aplicacion) { this.aplicacion = aplicacion; }

    public LocalDateTime getHora() { return hora; }
    public void setHora(LocalDateTime hora) { this.hora = hora; }

    public String getPasarelaPagos() { return pasarelaPagos; }
    public void setPasarelaPagos(String pasarelaPagos) { this.pasarelaPagos = pasarelaPagos; }

    public String getCortacircuitos() { return cortacircuitos; }
    public void setCortacircuitos(String cortacircuitos) { this.cortacircuitos = cortacircuitos; }

    public String getBaseDatos() {
        return baseDatos;
    }

    public void setBaseDatos(String baseDatos) {
        this.baseDatos = baseDatos;
    }
}
