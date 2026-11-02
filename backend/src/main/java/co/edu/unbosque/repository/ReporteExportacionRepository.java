package co.edu.unbosque.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.ReporteExportacion;

@Repository
public interface ReporteExportacionRepository extends CrudRepository<ReporteExportacion, Long>,
        PagingAndSortingRepository<ReporteExportacion, Long> {

    Page<ReporteExportacion> findByAdminIdOrderByCreadoEnDesc(Long adminId, Pageable pageable);

    /** Para retomar exportaciones que quedaron pendientes (por ejemplo, si la app se reinició). */
    List<ReporteExportacion> findByEstadoAndCreadoEnBefore(String estado, LocalDateTime fecha);
}
