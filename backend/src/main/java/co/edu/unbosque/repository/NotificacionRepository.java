package co.edu.unbosque.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Notificacion;

@Repository
public interface NotificacionRepository extends CrudRepository<Notificacion, Long>,
        PagingAndSortingRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId, Pageable pageable);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
