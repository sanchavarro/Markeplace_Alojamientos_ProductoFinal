package co.edu.unbosque.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Reserva;

@Repository
public interface ReservaRepository extends CrudRepository<Reserva, Long>, PagingAndSortingRepository<Reserva, Long> {

    /** HU-29: historial del huésped. */
    Page<Reserva> findByHuespedIdOrderByCreadaEnDesc(Long huespedId, Pageable pageable);

    /** HU-30: reservas de los alojamientos de un anfitrión. */
    @Query("SELECT r FROM Reserva r WHERE r.alojamientoId IN "
            + "(SELECT a.id FROM Alojamiento a WHERE a.anfitrionId = :anfitrionId) "
            + "AND (:alojamientoId = 0 OR r.alojamientoId = :alojamientoId) ORDER BY r.fechaLlegada DESC")
    Page<Reserva> buscarPorAnfitrion(@Param("anfitrionId") Long anfitrionId, @Param("alojamientoId") Long alojamientoId,
                                     Pageable pageable);

    List<Reserva> findByEstadoAndFechaSalidaLessThanEqual(String estado, LocalDate fecha);

    List<Reserva> findByEstadoAndCreadaEnBefore(String estado, LocalDateTime fecha);

    List<Reserva> findByCreadaEnBetweenOrderByCreadaEnAsc(LocalDateTime desde, LocalDateTime hasta);

    long countByEstado(String estado);

    /** HU-06: reservas vigentes del usuario como huésped. */
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.huespedId = :usuarioId AND r.estado IN :estados AND r.fechaSalida >= :hoy")
    long contarActivasComoHuesped(@Param("usuarioId") Long usuarioId, @Param("estados") List<String> estados,
                                  @Param("hoy") LocalDate hoy);

    /** HU-06: reservas vigentes en los alojamientos del usuario como anfitrión. */
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.estado IN :estados AND r.fechaSalida >= :hoy AND r.alojamientoId IN "
            + "(SELECT a.id FROM Alojamiento a WHERE a.anfitrionId = :usuarioId)")
    long contarActivasComoAnfitrion(@Param("usuarioId") Long usuarioId, @Param("estados") List<String> estados,
                                    @Param("hoy") LocalDate hoy);

    /** HU-42: reservas futuras de un alojamiento que siguen vigentes. */
    @Query("SELECT r FROM Reserva r WHERE r.alojamientoId = :alojamientoId AND r.estado IN :estados AND r.fechaLlegada >= :hoy")
    List<Reserva> buscarFuturas(@Param("alojamientoId") Long alojamientoId, @Param("estados") List<String> estados,
                                @Param("hoy") LocalDate hoy);

    /** HU-42: reservas marcadas para revisión manual del administrador. */
    Page<Reserva> findByRequiereRevisionTrueOrderByFechaLlegadaAsc(Pageable pageable);

    /** HU-46: ciudades con más reservas confirmadas o completadas. */
    @Query("SELECT a.ciudad, COUNT(r) FROM Reserva r, Alojamiento a WHERE r.alojamientoId = a.id "
            + "AND r.estado IN ('CONFIRMADA','COMPLETADA') GROUP BY a.ciudad ORDER BY COUNT(r) DESC")
    List<Object[]> ciudadesConMasReservas(Pageable pageable);
}
