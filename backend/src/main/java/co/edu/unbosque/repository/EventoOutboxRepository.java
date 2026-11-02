package co.edu.unbosque.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.EventoOutbox;

@Repository
public interface EventoOutboxRepository extends CrudRepository<EventoOutbox, Long> {

    List<EventoOutbox> findTop50ByEstadoOrderByCreadoEnAsc(String estado);

    long countByEstado(String estado);
}
