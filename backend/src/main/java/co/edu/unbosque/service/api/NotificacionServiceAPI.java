package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.Notificacion;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * Notificaciones internas del usuario (HU-36, HU-37).
 */
public interface NotificacionServiceAPI extends GenericServiceAPI<Notificacion, Long> {

    Notificacion crear(Long usuarioId, String tipo, String mensaje);

    PaginaResponse<Notificacion> listar(Long usuarioId, Integer pagina, Integer tamano);

    Notificacion marcarLeida(Long usuarioId, Long notificacionId);

    int marcarTodasLeidas(Long usuarioId);

    long contarNoLeidas(Long usuarioId);
}
