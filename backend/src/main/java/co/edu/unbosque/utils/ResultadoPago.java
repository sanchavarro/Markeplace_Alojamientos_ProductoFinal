package co.edu.unbosque.utils;

/**
 * Respuesta de la pasarela de pagos, independiente de si es Stripe o el simulador.
 */
public class ResultadoPago {

    private boolean aprobado;
    private String referencia;
    private String mensaje;

    public ResultadoPago() {
    }

    public ResultadoPago(boolean aprobado, String referencia, String mensaje) {
        this.aprobado = aprobado;
        this.referencia = referencia;
        this.mensaje = mensaje;
    }

    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean aprobado) { this.aprobado = aprobado; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
