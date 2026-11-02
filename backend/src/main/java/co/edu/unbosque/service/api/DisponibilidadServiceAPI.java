package co.edu.unbosque.service.api;

import java.time.LocalDate;
import java.util.List;
import co.edu.unbosque.dto.PeriodoRequest;
import co.edu.unbosque.entity.PeriodoDisponibilidad;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * IDisponibilidad del diagrama de componentes (épica E04, HU-16 a HU-18).
 * Los periodos se manejan como intervalos [fechaInicio, fechaFin): fechaFin es la fecha de salida.
 */
public interface DisponibilidadServiceAPI extends GenericServiceAPI<PeriodoDisponibilidad, Long> {

    PeriodoDisponibilidad crearPeriodo(Long anfitrionId, Long alojamientoId, PeriodoRequest request);

    /** HU-16: cambia fechas o estado de un periodo sin dejar reservas por fuera. */
    PeriodoDisponibilidad editarPeriodo(Long anfitrionId, Long alojamientoId, Long periodoId, PeriodoRequest request);

    void eliminarPeriodo(Long anfitrionId, Long alojamientoId, Long periodoId);

    List<PeriodoDisponibilidad> calendario(Long alojamientoId, LocalDate desde, LocalDate hasta);

    boolean estaDisponible(Long alojamientoId, LocalDate llegada, LocalDate salida);

    void bloquearPorReserva(Long alojamientoId, Long reservaId, LocalDate llegada, LocalDate salida);

    void liberarPorReserva(Long reservaId);
}
