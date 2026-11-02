package co.edu.unbosque.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.EventoAuditoria;

/**
 * Solo se usan save y consultas: la auditoría no se edita ni se borra (ADR-06).
 */
@Repository
public interface EventoAuditoriaRepository extends CrudRepository<EventoAuditoria, Long>,
        PagingAndSortingRepository<EventoAuditoria, Long> {

    Page<EventoAuditoria> findAllByOrderByFechaDesc(Pageable pageable);

    Page<EventoAuditoria> findByEntidadOrderByFechaDesc(String entidad, Pageable pageable);

    Page<EventoAuditoria> findByUsuarioIdOrderByFechaDesc(Long usuarioId, Pageable pageable);

    /** HU-45: filtros combinables por usuario, entidad, acción y rango de fechas. */
    @Query("SELECT e FROM EventoAuditoria e WHERE (:entidad = '' OR e.entidad = :entidad) "
            + "AND (:usuarioId = 0 OR e.usuarioId = :usuarioId) AND (:accion = '' OR e.accion = :accion) "
            + "AND e.fecha >= :desde AND e.fecha < :hasta ORDER BY e.fecha DESC")
    Page<EventoAuditoria> filtrar(@Param("entidad") String entidad, @Param("usuarioId") Long usuarioId,
                                  @Param("accion") String accion, @Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta, Pageable pageable);
}
