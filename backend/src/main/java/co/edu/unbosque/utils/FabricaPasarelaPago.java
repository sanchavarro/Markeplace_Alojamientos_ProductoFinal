package co.edu.unbosque.utils;

/**
 * PATRÓN ABSTRACT FACTORY.
 * Cada pasarela (Stripe, simulador, y en el futuro PayU o Mercado Pago) entrega su familia completa
 * de objetos: el que cobra y el que reembolsa. Así nunca se mezcla el cobro de una pasarela
 * con el reembolso de otra, y cambiar de pasarela es cambiar de fábrica.
 */
public interface FabricaPasarelaPago {

    ProcesadorCobro crearProcesadorCobro();

    ProcesadorReembolso crearProcesadorReembolso();

    String nombre();
}
