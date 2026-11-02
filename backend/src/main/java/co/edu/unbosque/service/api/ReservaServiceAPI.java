package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.ReservaRequest;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * IReservas del diagrama de componentes (épica E07, HU-26 a HU-31).
 * ADR-02 bloqueo pesimista, ADR-03 idempotencia, ADR-04 pago con tiempo límite y cortacircuitos.
 */
public interface ReservaServiceAPI extends GenericServiceAPI<Reserva, Long> {

    ReservaResponse reservar(Long huespedId, ReservaRequest request, String claveIdempotencia);

    ReservaResponse pagar(Long huespedId, Long reservaId, String metodoPago, String claveIdempotencia);

    ReservaResponse aprobar(Long anfitrionId, Long reservaId);

    ReservaResponse rechazar(Long anfitrionId, Long reservaId);

    PaginaResponse<ReservaResponse> historialHuesped(Long huespedId, Integer pagina, Integer tamano);

    /** HU-30: reservas de mis alojamientos; alojamientoId es opcional para ver uno solo. */
    PaginaResponse<ReservaResponse> reservasAnfitrion(Long anfitrionId, Long alojamientoId, Integer pagina, Integer tamano);

    /** HU-42: reservas marcadas para revisión manual porque su publicación fue bloqueada. */
    PaginaResponse<ReservaResponse> enRevisionManual(Integer pagina, Integer tamano);

    /** HU-42: el administrador decide mantener la reserva (quita la marca de revisión). */
    ReservaResponse mantenerTrasRevision(Long adminId, Long reservaId, String motivo);

    ReservaResponse detalle(Long usuarioId, String rol, Long reservaId);

    Reserva obtener(Long reservaId);

    ReservaResponse aResponse(Reserva reserva, String mensaje);

    int completarReservasFinalizadas();

    int vencerPagosPendientes();

    int limpiarClavesIdempotencia();
}
