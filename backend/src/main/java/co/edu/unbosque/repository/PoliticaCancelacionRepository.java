package co.edu.unbosque.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.PoliticaCancelacion;

@Repository
public interface PoliticaCancelacionRepository extends CrudRepository<PoliticaCancelacion, Long> {

    Optional<PoliticaCancelacion> findFirstByTipo(String tipo);
}
