package co.edu.unbosque.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.entity.Pago;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.service.api.PagoServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.PasarelaPagoAdapter;
import co.edu.unbosque.utils.ResultadoPago;

@Service
public class PagoServiceImpl extends GenericServiceImpl<Pago, Long> implements PagoServiceAPI {

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PasarelaPagoAdapter pasarelaPagoAdapter;

    @Override
    public CrudRepository<Pago, Long> getDao() {
        return pagoRepository;
    }

    /**
     * La llamada a la pasarela se hace FUERA de la transacción de la reserva,
     * para no mantener el bloqueo de la base de datos mientras se espera a Stripe (hasta 10 s).
     */
    @Override
    public ResultadoPago cobrar(BigDecimal monto, String metodoPago, UUID claveIdempotencia, String descripcion) {
        return pasarelaPagoAdapter.cobrar(monto, metodoPago, claveIdempotencia.toString(), descripcion);
    }

    @Override
    public ResultadoPago reembolsar(Pago pago, BigDecimal monto, String claveIdempotencia) {
        return pasarelaPagoAdapter.reembolsar(pago.getReferenciaStripe(), monto, claveIdempotencia);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Pago registrar(Long reservaId, BigDecimal monto, String estado, String referencia, UUID claveStripe) {
        Pago pago = new Pago();
        pago.setReservaId(reservaId);
        pago.setMonto(monto);
        pago.setEstado(estado);
        pago.setReferenciaStripe(referencia);
        pago.setClaveStripe(claveStripe);
        pago.setCreadoEn(LocalDateTime.now());
        return pagoRepository.save(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public Pago ultimoPago(Long reservaId) {
        List<Pago> pagos = pagoRepository.findByReservaIdOrderByCreadoEnDesc(reservaId);
        return pagos.isEmpty() ? null : pagos.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Pago pagoAprobado(Long reservaId) {
        return pagoRepository.findFirstByReservaIdAndEstado(reservaId, Constantes.PAGO_APROBADO).orElse(null);
    }
}
