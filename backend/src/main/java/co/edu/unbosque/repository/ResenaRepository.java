package co.edu.unbosque.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Resena;

@Repository
public interface ResenaRepository extends CrudRepository<Resena, Long>, PagingAndSortingRepository<Resena, Long> {

    boolean existsByReservaId(Long reservaId);

    Page<Resena> findByAlojamientoIdAndVisibleTrueOrderByCreadaEnDesc(Long alojamientoId, Pageable pageable);

    @Query("SELECT AVG(r.calificacion) FROM Resena r WHERE r.alojamientoId = :alojamientoId AND r.visible = TRUE")
    Double promedioPorAlojamiento(@Param("alojamientoId") Long alojamientoId);

    long countByAlojamientoIdAndVisibleTrue(Long alojamientoId);
}
