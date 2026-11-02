package co.edu.unbosque.service.impl;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.PeriodoRequest;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.PeriodoDisponibilidad;
import co.edu.unbosque.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.DisponibilidadServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class DisponibilidadServiceImpl extends GenericServiceImpl<PeriodoDisponibilidad, Long>
        implements DisponibilidadServiceAPI {

    @Autowired
    private PeriodoDisponibilidadRepository periodoRepository;

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Override
    public CrudRepository<PeriodoDisponibilidad, Long> getDao() {
        return periodoRepository;
    }

    /** HU-16: el anfitrión abre fechas (DISPONIBLE) o cierra fechas (BLOQUEADO). */
    @Override
    @Transactional
    public PeriodoDisponibilidad crearPeriodo(Long anfitrionId, Long alojamientoId, PeriodoRequest request) {
        Alojamiento alojamiento = alojamientoServiceAPI.obtenerPropio(anfitrionId, alojamientoId);
        if (Constantes.ALOJAMIENTO_BLOQUEADA.equals(alojamiento.getEstado())) {
            throw new ReglaNegocioException("La publicación está bloqueada.");
        }
        String estado = request.getEstado().trim().toUpperCase();
        if (!Constantes.PERIODO_DISPONIBLE.equals(estado) && !Constantes.PERIODO_BLOQUEADO.equals(estado)) {
            throw new ReglaNegocioException("El estado del periodo debe ser DISPONIBLE o BLOQUEADO.");
        }
        LocalDate inicio = request.getFechaInicio();
        LocalDate fin = request.getFechaFin();
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se pueden crear periodos en fechas pasadas.");
        }
        if (Constantes.PERIODO_DISPONIBLE.equals(estado)
                && periodoRepository.contarSolapados(alojamientoId, List.of(Constantes.PERIODO_DISPONIBLE), inicio, fin) > 0) {
            throw new ConflictoException("Ya existe un periodo DISPONIBLE que se cruza con esas fechas.");
        }
        if (Constantes.PERIODO_BLOQUEADO.equals(estado)
                && periodoRepository.contarSolapados(alojamientoId,
                        List.of(Constantes.PERIODO_RESERVADO, Constantes.PERIODO_BLOQUEADO), inicio, fin) > 0) {
            throw new ConflictoException("Esas fechas ya tienen reservas o bloqueos.");
        }
        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
        periodo.setAlojamientoId(alojamientoId);
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        periodo.setEstado(estado);
        periodo = periodoRepository.save(periodo);
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "PERIODO_DISPONIBILIDAD", periodo.getId(), "CREAR_PERIODO",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("alojamientoId", alojamientoId, "estado", estado,
                        "inicio", inicio.toString(), "fin", fin.toString()));
        return periodo;
    }

    @Override
    @Transactional
    public void eliminarPeriodo(Long anfitrionId, Long alojamientoId, Long periodoId) {
        alojamientoServiceAPI.obtenerPropio(anfitrionId, alojamientoId);
        PeriodoDisponibilidad periodo = periodoRepository.findById(periodoId)
                .filter(p -> p.getAlojamientoId().equals(alojamientoId))
                .orElseThrow(() -> new ResourceNotFoundException("Periodo no encontrado en este alojamiento."));
        if (Constantes.PERIODO_RESERVADO.equals(periodo.getEstado())) {
            throw new ReglaNegocioException("Un periodo RESERVADO no se puede eliminar; se libera al cancelar la reserva.");
        }
        if (Constantes.PERIODO_DISPONIBLE.equals(periodo.getEstado()) && periodoRepository.contarSolapados(alojamientoId,
                List.of(Constantes.PERIODO_RESERVADO), periodo.getFechaInicio(), periodo.getFechaFin()) > 0) {
            throw new ConflictoException("No puedes eliminar este periodo porque tiene reservas confirmadas.");
        }
        periodoRepository.delete(periodo);
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "PERIODO_DISPONIBILIDAD", periodoId, "ELIMINAR_PERIODO",
                Constantes.AUDITORIA_EXITO, null);
    }

    /**
     * HU-16: editar un periodo. Si es DISPONIBLE, el nuevo rango debe seguir cubriendo las reservas que ya tiene.
     */
    @Override
    @Transactional
    public PeriodoDisponibilidad editarPeriodo(Long anfitrionId, Long alojamientoId, Long periodoId,
                                               PeriodoRequest request) {
        alojamientoServiceAPI.obtenerPropio(anfitrionId, alojamientoId);
        PeriodoDisponibilidad periodo = periodoRepository.findById(periodoId)
                .filter(p -> p.getAlojamientoId().equals(alojamientoId))
                .orElseThrow(() -> new ResourceNotFoundException("Periodo no encontrado en este alojamiento."));
        if (Constantes.PERIODO_RESERVADO.equals(periodo.getEstado())) {
            throw new ReglaNegocioException("Un periodo RESERVADO se gestiona desde la reserva, no se edita.");
        }
        String estado = request.getEstado().trim().toUpperCase();
        if (!Constantes.PERIODO_DISPONIBLE.equals(estado) && !Constantes.PERIODO_BLOQUEADO.equals(estado)) {
            throw new ReglaNegocioException("El estado del periodo debe ser DISPONIBLE o BLOQUEADO.");
        }
        LocalDate inicio = request.getFechaInicio();
        LocalDate fin = request.getFechaFin();
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (!fin.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("El periodo debe terminar después de hoy.");
        }
        // Las reservas que estaban dentro del periodo deben seguir dentro del nuevo rango disponible.
        if (Constantes.PERIODO_DISPONIBLE.equals(periodo.getEstado())) {
            for (PeriodoDisponibilidad reservado : periodoRepository.buscarSolapados(alojamientoId,
                    List.of(Constantes.PERIODO_RESERVADO), periodo.getFechaInicio(), periodo.getFechaFin())) {
                boolean cubierto = Constantes.PERIODO_DISPONIBLE.equals(estado)
                        && !reservado.getFechaInicio().isBefore(inicio) && !reservado.getFechaFin().isAfter(fin);
                if (!cubierto) {
                    throw new ConflictoException("El cambio deja por fuera reservas confirmadas ("
                            + reservado.getFechaInicio() + " a " + reservado.getFechaFin() + ").");
                }
            }
        }
        List<String> chocan = Constantes.PERIODO_DISPONIBLE.equals(estado)
                ? List.of(Constantes.PERIODO_DISPONIBLE)
                : List.of(Constantes.PERIODO_RESERVADO, Constantes.PERIODO_BLOQUEADO);
        if (periodoRepository.contarSolapadosExcepto(alojamientoId, periodoId, chocan, inicio, fin) > 0) {
            throw new ConflictoException("Las nuevas fechas se cruzan con otro periodo.");
        }
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        periodo.setEstado(estado);
        periodo = periodoRepository.save(periodo);
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "PERIODO_DISPONIBILIDAD", periodoId, "EDITAR_PERIODO",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("estado", estado, "inicio", inicio.toString(),
                        "fin", fin.toString()));
        return periodo;
    }

    /** HU-17: calendario del alojamiento entre dos fechas. */
    @Override
    @Transactional(readOnly = true)
    public List<PeriodoDisponibilidad> calendario(Long alojamientoId, LocalDate desde, LocalDate hasta) {
        alojamientoServiceAPI.obtener(alojamientoId);
        LocalDate inicio = desde == null ? LocalDate.now() : desde;
        LocalDate fin = hasta == null ? inicio.plusMonths(6) : hasta;
        return periodoRepository
                .findByAlojamientoIdAndFechaFinGreaterThanEqualAndFechaInicioLessThanEqualOrderByFechaInicioAsc(
                        alojamientoId, inicio, fin);
    }

    /** HU-27: hay un periodo DISPONIBLE que cubre la estadía y nada RESERVADO/BLOQUEADO se cruza con ella. */
    @Override
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long alojamientoId, LocalDate llegada, LocalDate salida) {
        boolean cubierto = periodoRepository.contarPeriodosQueCubren(alojamientoId, llegada, salida) > 0;
        boolean ocupado = periodoRepository.contarSolapados(alojamientoId,
                List.of(Constantes.PERIODO_RESERVADO, Constantes.PERIODO_BLOQUEADO), llegada, salida) > 0;
        return cubierto && !ocupado;
    }

    /** HU-18: bloqueo automático de las fechas reservadas. Solo se llama dentro de la transacción de la reserva. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquearPorReserva(Long alojamientoId, Long reservaId, LocalDate llegada, LocalDate salida) {
        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
        periodo.setAlojamientoId(alojamientoId);
        periodo.setReservaId(reservaId);
        periodo.setFechaInicio(llegada);
        periodo.setFechaFin(salida);
        periodo.setEstado(Constantes.PERIODO_RESERVADO);
        periodoRepository.save(periodo);
    }

    /** HU-35: liberar el periodo cuando la reserva se cancela o el pago falla. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void liberarPorReserva(Long reservaId) {
        periodoRepository.eliminarPorReserva(reservaId);
    }
}
