package co.edu.unbosque.service.api;

import java.util.List;
import co.edu.unbosque.entity.EventoOutbox;

/**
 * IPublicadorEventos del diagrama de componentes (ADR-07, PAT-04 Observador + Outbox).
 * El negocio no envía notificaciones directamente: deja un evento pendiente en la misma transacción.
 */
public interface EventoOutboxServiceAPI {

    void publicar(String tipo, Long usuarioId, String titulo, String mensaje);

    /** Igual que publicar, con un enlace que solo se envía por correo. */
    void publicar(String tipo, Long usuarioId, String titulo, String mensaje, String enlace);

    List<EventoOutbox> pendientes();

    void marcarProcesado(Long eventoId);

    void registrarFallo(Long eventoId, int intentosMaximos);

    long contarPorEstado(String estado);
}
