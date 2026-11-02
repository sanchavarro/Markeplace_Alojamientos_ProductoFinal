package co.edu.unbosque.service.api;

import java.math.BigDecimal;
import java.util.UUID;
import co.edu.unbosque.entity.Pago;
import co.edu.unbosque.utils.GenericServiceAPI;
import co.edu.unbosque.utils.ResultadoPago;

/**
 * IPagos del diagrama de componentes (HU-31, ADR-03 y ADR-04).
 */
public interface PagoServiceAPI extends GenericServiceAPI<Pago, Long> {

    /** Cobra a través del adaptador (fábrica abstracta + cortacircuitos). No abre transacción. */
    ResultadoPago cobrar(BigDecimal monto, String metodoPago, UUID claveIdempotencia, String descripcion);

    /** Reembolsa a través del adaptador. No abre transacción. */
    ResultadoPago reembolsar(Pago pago, BigDecimal monto, String claveIdempotencia);

    Pago registrar(Long reservaId, BigDecimal monto, String estado, String referencia, UUID claveStripe);

    Pago ultimoPago(Long reservaId);

    Pago pagoAprobado(Long reservaId);
}
