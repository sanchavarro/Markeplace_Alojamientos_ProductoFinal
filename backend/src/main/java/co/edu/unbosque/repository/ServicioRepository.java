package co.edu.unbosque.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Servicio;

@Repository
public interface ServicioRepository extends CrudRepository<Servicio, Long> {

    Optional<Servicio> findByNombreIgnoreCase(String nombre);
}
