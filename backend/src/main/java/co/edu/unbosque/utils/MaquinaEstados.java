package co.edu.unbosque.utils;

import java.util.Map;
import java.util.Set;

/**
 * PATRÓN ESTADO (PAT-06) implementado como máquina de estados.
 * Define qué cambios de estado son válidos para una reserva y para una publicación.
 * Cualquier otro cambio se rechaza con ReglaNegocioException (422).
 */
public final class MaquinaEstados {

    private MaquinaEstados() {
    }

    private static final Map<String, Set<String>> RESERVA = Map.of(
            Constantes.RESERVA_EN_REVISION, Set.of(Constantes.RESERVA_PENDIENTE_PAGO, Constantes.RESERVA_CANCELADA),
            Constantes.RESERVA_PENDIENTE_PAGO, Set.of(Constantes.RESERVA_CONFIRMADA, Constantes.RESERVA_PAGO_FALLIDO,
                    Constantes.RESERVA_CANCELADA),
            Constantes.RESERVA_PAGO_FALLIDO, Set.of(Constantes.RESERVA_CANCELADA),
            Constantes.RESERVA_CONFIRMADA, Set.of(Constantes.RESERVA_CANCELADA, Constantes.RESERVA_COMPLETADA),
            Constantes.RESERVA_CANCELADA, Set.of(),
            Constantes.RESERVA_COMPLETADA, Set.of());

    /**
     * BORRADOR → PENDIENTE_REVISION → ACTIVA ⇄ PAUSADA.
     * El administrador puede devolver una pendiente a BORRADOR o bloquear; al desbloquear vuelve a revisión.
     */
    private static final Map<String, Set<String>> PUBLICACION = Map.of(
            Constantes.ALOJAMIENTO_BORRADOR, Set.of(Constantes.ALOJAMIENTO_PENDIENTE_REVISION),
            Constantes.ALOJAMIENTO_PENDIENTE_REVISION, Set.of(Constantes.ALOJAMIENTO_ACTIVA,
                    Constantes.ALOJAMIENTO_BORRADOR, Constantes.ALOJAMIENTO_BLOQUEADA),
            Constantes.ALOJAMIENTO_ACTIVA, Set.of(Constantes.ALOJAMIENTO_PAUSADA, Constantes.ALOJAMIENTO_BLOQUEADA),
            Constantes.ALOJAMIENTO_PAUSADA, Set.of(Constantes.ALOJAMIENTO_ACTIVA, Constantes.ALOJAMIENTO_BLOQUEADA),
            Constantes.ALOJAMIENTO_BLOQUEADA, Set.of(Constantes.ALOJAMIENTO_PENDIENTE_REVISION));

    public static void validarReserva(String actual, String nuevo) {
        validar(RESERVA, actual, nuevo, "La reserva");
    }

    public static void validarPublicacion(String actual, String nuevo) {
        validar(PUBLICACION, actual, nuevo, "La publicación");
    }

    private static void validar(Map<String, Set<String>> tabla, String actual, String nuevo, String sujeto) {
        Set<String> permitidos = tabla.get(actual);
        if (permitidos == null || !permitidos.contains(nuevo)) {
            throw new ReglaNegocioException(sujeto + " no puede pasar de " + actual + " a " + nuevo + ".");
        }
    }
}
