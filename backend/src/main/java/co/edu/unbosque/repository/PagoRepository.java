package co.edu.unbosque.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.Pago;

@Repository
public interface PagoRepository extends CrudRepository<Pago, Long> {

    List<Pago> findByReservaIdOrderByCreadoEnDesc(Long reservaId);

    Optional<Pago> findFirstByReservaIdAndEstado(Long reservaId, String estado);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'APROBADO'")
    BigDecimal sumarAprobados();
}
