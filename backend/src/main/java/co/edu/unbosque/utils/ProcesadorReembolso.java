package co.edu.unbosque.utils;

import java.math.BigDecimal;

/**
 * Producto B de la fábrica abstracta de pagos: devuelve dinero de un cobro anterior.
 */
public interface ProcesadorReembolso {

    ResultadoPago reembolsar(String referenciaPago, BigDecimal monto, String claveIdempotencia);
}
