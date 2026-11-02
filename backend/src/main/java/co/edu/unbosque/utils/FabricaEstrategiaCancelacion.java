package co.edu.unbosque.utils;

/**
 * Entrega la estrategia de cancelación según el tipo de política guardado en politica_cancelacion.tipo.
 */
public final class FabricaEstrategiaCancelacion {

    private FabricaEstrategiaCancelacion() {
    }

    public static EstrategiaCancelacion obtener(String tipoPolitica) {
        if (Constantes.POLITICA_ESTRICTA.equals(tipoPolitica)) {
            return new CancelacionEstricta();
        }
        return new CancelacionFlexible();
    }
}
