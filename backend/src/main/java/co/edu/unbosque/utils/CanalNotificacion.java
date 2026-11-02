package co.edu.unbosque.utils;

import co.edu.unbosque.dto.CargaNotificacion;

/**
 * Producto del Factory Method: el medio por el que llega el aviso al usuario.
 */
public interface CanalNotificacion {

    void enviar(String tipoEvento, CargaNotificacion carga);
}
