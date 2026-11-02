package co.edu.unbosque.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Imagen;

@Repository
public interface ImagenRepository extends CrudRepository<Imagen, Long> {

    List<Imagen> findByAlojamientoIdOrderByOrdenAsc(Long alojamientoId);

    long countByAlojamientoId(Long alojamientoId);
}
