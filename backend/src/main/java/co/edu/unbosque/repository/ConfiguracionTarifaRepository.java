package co.edu.unbosque.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.ConfiguracionTarifa;

@Repository
public interface ConfiguracionTarifaRepository extends CrudRepository<ConfiguracionTarifa, Long> {

    Optional<ConfiguracionTarifa> findByClave(String clave);
}
