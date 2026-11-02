package co.edu.unbosque.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.ServicioAdicional;

@Repository
public interface ServicioAdicionalRepository extends CrudRepository<ServicioAdicional, Long> {

    List<ServicioAdicional> findByAlojamientoId(Long alojamientoId);
}
