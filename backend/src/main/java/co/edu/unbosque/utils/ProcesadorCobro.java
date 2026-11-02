package co.edu.unbosque.utils;

import java.math.BigDecimal;

/**
 * Producto A de la fábrica abstracta de pagos: realiza un cobro.
 * Si la tarjeta es rechazada lanza PagoRechazadoException; si la pasarela falla lanza RuntimeException.
 */
public interface ProcesadorCobro {

    ResultadoPago cobrar(BigDecimal monto, String metodoPago, String claveIdempotencia, String descripcion);
}
