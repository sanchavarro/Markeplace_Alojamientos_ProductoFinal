package co.edu.unbosque.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.PeriodoDisponibilidad;

@Repository
public interface PeriodoDisponibilidadRepository extends CrudRepository<PeriodoDisponibilidad, Long> {

    List<PeriodoDisponibilidad> findByAlojamientoIdOrderByFechaInicioAsc(Long alojamientoId);

    List<PeriodoDisponibilidad> findByAlojamientoIdAndFechaFinGreaterThanEqualAndFechaInicioLessThanEqualOrderByFechaInicioAsc(
            Long alojamientoId, LocalDate desde, LocalDate hasta);

    /** ¿Existe un periodo DISPONIBLE que cubra toda la estadía? */
    @Query("SELECT COUNT(p) FROM PeriodoDisponibilidad p WHERE p.alojamientoId = :alojamientoId "
            + "AND p.estado = 'DISPONIBLE' AND p.fechaInicio <= :llegada AND p.fechaFin >= :salida")
    long contarPeriodosQueCubren(@Param("alojamientoId") Long alojamientoId,
                                 @Param("llegada") LocalDate llegada,
                                 @Param("salida") LocalDate salida);

    /** Periodos de un estado que se cruzan con [inicio, fin) (sin superposiciones, HU-27). */
    @Query("SELECT COUNT(p) FROM PeriodoDisponibilidad p WHERE p.alojamientoId = :alojamientoId "
            + "AND p.estado IN :estados AND p.fechaInicio < :fin AND p.fechaFin > :inicio")
    long contarSolapados(@Param("alojamientoId") Long alojamientoId,
                         @Param("estados") List<String> estados,
                         @Param("inicio") LocalDate inicio,
                         @Param("fin") LocalDate fin);

    /** HU-16: igual que contarSolapados pero sin contar el periodo que se está editando. */
    @Query("SELECT COUNT(p) FROM PeriodoDisponibilidad p WHERE p.alojamientoId = :alojamientoId AND p.id <> :excluirId "
            + "AND p.estado IN :estados AND p.fechaInicio < :fin AND p.fechaFin > :inicio")
    long contarSolapadosExcepto(@Param("alojamientoId") Long alojamientoId,
                                @Param("excluirId") Long excluirId,
                                @Param("estados") List<String> estados,
                                @Param("inicio") LocalDate inicio,
                                @Param("fin") LocalDate fin);

    /** HU-16: periodos RESERVADOS dentro de un rango (para no dejar reservas sin cobertura). */
    @Query("SELECT p FROM PeriodoDisponibilidad p WHERE p.alojamientoId = :alojamientoId "
            + "AND p.estado IN :estados AND p.fechaInicio < :fin AND p.fechaFin > :inicio")
    List<PeriodoDisponibilidad> buscarSolapados(@Param("alojamientoId") Long alojamientoId,
                         @Param("estados") List<String> estados,
                         @Param("inicio") LocalDate inicio,
                         @Param("fin") LocalDate fin);

    @Modifying
    @Query("DELETE FROM PeriodoDisponibilidad p WHERE p.reservaId = :reservaId")
    int eliminarPorReserva(@Param("reservaId") Long reservaId);
}
