package co.edu.unbosque.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.AlojamientoServicio;
import co.edu.unbosque.entity.AlojamientoServicioId;

@Repository
public interface AlojamientoServicioRepository extends CrudRepository<AlojamientoServicio, AlojamientoServicioId> {

    List<AlojamientoServicio> findByAlojamientoId(Long alojamientoId);

    @Modifying
    @Query("DELETE FROM AlojamientoServicio s WHERE s.alojamientoId = :alojamientoId")
    void eliminarPorAlojamiento(@Param("alojamientoId") Long alojamientoId);
}
