package co.edu.unbosque.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.edu.unbosque.entity.ClaveIdempotencia;

@Repository
public interface ClaveIdempotenciaRepository extends CrudRepository<ClaveIdempotencia, UUID> {

    /**
     * Inserta la clave de inmediato (no espera al final de la transacción).
     * Si otra petición ya insertó la misma clave, PostgreSQL lanza un error de llave duplicada (ADR-03).
     */
    @Modifying
    @Query(value = "INSERT INTO clave_idempotencia (clave, usuario_id, expira_en) VALUES (:clave, :usuarioId, :expiraEn)",
            nativeQuery = true)
    int insertar(@Param("clave") UUID clave, @Param("usuarioId") Long usuarioId,
                 @Param("expiraEn") LocalDateTime expiraEn);

    /** Borra las claves vencidas (más de 24 h, ADR-03). */
    @Modifying
    @Query("DELETE FROM ClaveIdempotencia c WHERE c.expiraEn < :ahora")
    int eliminarExpiradas(@Param("ahora") LocalDateTime ahora);
}
