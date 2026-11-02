package co.edu.unbosque.service.api;

import java.time.LocalDate;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.EventoAuditoria;

/**
 * IAuditoria del diagrama de componentes (ADR-06, T14 Mantener rastro de auditoría).
 * Solo se puede registrar y consultar: no hay métodos para editar ni borrar.
 */
public interface EventoAuditoriaServiceAPI {

    /** Debe llamarse dentro de una transacción activa: si la operación falla, la auditoría también se revierte. */
    void registrar(Long usuarioId, String entidad, Long entidadId, String accion, String resultado, String detalle);

    PaginaResponse<EventoAuditoria> consultar(String entidad, Long usuarioId, String accion, LocalDate desde,
                                              LocalDate hasta, Integer pagina, Integer tamano);
}
