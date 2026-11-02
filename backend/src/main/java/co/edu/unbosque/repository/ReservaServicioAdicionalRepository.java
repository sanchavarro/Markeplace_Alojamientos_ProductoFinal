package co.edu.unbosque.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.ReservaServicioAdicional;
import co.edu.unbosque.entity.ReservaServicioAdicionalId;

@Repository
public interface ReservaServicioAdicionalRepository
        extends CrudRepository<ReservaServicioAdicional, ReservaServicioAdicionalId> {

    List<ReservaServicioAdicional> findByReservaId(Long reservaId);
}
