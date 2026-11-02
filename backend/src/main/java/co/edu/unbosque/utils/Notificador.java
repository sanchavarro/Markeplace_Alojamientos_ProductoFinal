package co.edu.unbosque.utils;

import co.edu.unbosque.dto.CargaNotificacion;

/**
 * PATRÓN FACTORY METHOD.
 * La clase base define el algoritmo para notificar, pero deja que cada subclase decida
 * qué canal crear con crearCanal(). Así, NotificadorInterno crea un canal que guarda la notificación
 * en la base de datos y NotificadorCorreo crea un canal de correo, sin cambiar este código.
 * Además, cada Notificador es un observador del outbox (PAT-04).
 */
public abstract class Notificador implements ObservadorEvento {

    /** Factory method. */
    protected abstract CanalNotificacion crearCanal();

    @Override
    public void notificar(String tipoEvento, CargaNotificacion carga) {
        if (carga == null || carga.getUsuarioId() == null) {
            return;
        }
        CanalNotificacion canal = crearCanal();
        canal.enviar(tipoEvento, carga);
    }
}
