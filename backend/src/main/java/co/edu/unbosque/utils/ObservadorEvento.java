package co.edu.unbosque.utils;

import co.edu.unbosque.dto.CargaNotificacion;

/**
 * PATRÓN OBSERVADOR (PAT-04).
 * Cada observador se suscribe al despachador del outbox y reacciona cuando sale un evento.
 * Para agregar un canal nuevo (SMS, WhatsApp) se crea otro observador sin tocar el negocio.
 */
public interface ObservadorEvento {

    void notificar(String tipoEvento, CargaNotificacion carga);
}
