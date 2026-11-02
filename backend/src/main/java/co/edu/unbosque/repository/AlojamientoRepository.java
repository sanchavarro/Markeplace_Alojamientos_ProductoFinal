package co.edu.unbosque.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Alojamiento;
import jakarta.persistence.LockModeType;

@Repository
public interface AlojamientoRepository extends CrudRepository<Alojamiento, Long>,
        PagingAndSortingRepository<Alojamiento, Long> {

    /**
     * ADR-02 Bloqueo pesimista: SELECT ... FOR UPDATE sobre el alojamiento.
     * Mientras una transacción tenga este candado, otra reserva del mismo alojamiento espera.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alojamiento a WHERE a.id = :id")
    Optional<Alojamiento> bloquearPorId(@Param("id") Long id);

    Page<Alojamiento> findByAnfitrionIdOrderByCreadoEnDesc(Long anfitrionId, Pageable pageable);

    Page<Alojamiento> findByEstadoOrderByCreadoEnAsc(String estado, Pageable pageable);

    List<Alojamiento> findByIdIn(List<Long> ids);

    long countByEstado(String estado);

    /**
     * HU-19 a HU-21: búsqueda de publicaciones ACTIVAS con filtros opcionales y disponibilidad (ADR-08: paginada).
     * Los filtros vacíos llegan como '' o con valores neutros para evitar parámetros nulos en PostgreSQL.
     */
    @Query("SELECT a FROM Alojamiento a WHERE a.estado = 'ACTIVA' "
            + "AND EXISTS (SELECT u.id FROM Usuario u WHERE u.id = a.anfitrionId AND u.estado = 'ACTIVO') "
            + "AND (:ciudad = '' OR LOWER(a.ciudad) = LOWER(:ciudad)) "
            + "AND (:tipo = '' OR LOWER(a.tipo) = LOWER(:tipo)) "
            + "AND a.capacidad >= :capacidad "
            + "AND a.precioNoche BETWEEN :precioMin AND :precioMax "
            + "AND (:filtrarFechas = FALSE OR ("
            + "  EXISTS (SELECT p.id FROM PeriodoDisponibilidad p WHERE p.alojamientoId = a.id "
            + "          AND p.estado = 'DISPONIBLE' AND p.fechaInicio <= :llegada AND p.fechaFin >= :salida) "
            + "  AND NOT EXISTS (SELECT p2.id FROM PeriodoDisponibilidad p2 WHERE p2.alojamientoId = a.id "
            + "          AND p2.estado <> 'DISPONIBLE' AND p2.fechaInicio < :salida AND p2.fechaFin > :llegada))) "
            + "AND (:totalServicios = 0 OR (SELECT COUNT(s) FROM AlojamientoServicio s "
            + "     WHERE s.alojamientoId = a.id AND s.servicioId IN :servicios) = :totalServicios) "
            + "ORDER BY a.precioNoche ASC")
    Page<Alojamiento> buscar(@Param("ciudad") String ciudad,
                             @Param("tipo") String tipo,
                             @Param("capacidad") Integer capacidad,
                             @Param("precioMin") BigDecimal precioMin,
                             @Param("precioMax") BigDecimal precioMax,
                             @Param("filtrarFechas") boolean filtrarFechas,
                             @Param("llegada") LocalDate llegada,
                             @Param("salida") LocalDate salida,
                             @Param("servicios") List<Long> servicios,
                             @Param("totalServicios") long totalServicios,
                             Pageable pageable);
}
