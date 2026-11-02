package co.edu.unbosque.service.impl;

import co.edu.unbosque.utils.ServicioNoDisponibleException;
import co.edu.unbosque.utils.ConflictoException;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import co.edu.unbosque.dto.CancelacionResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.Cancelacion;
import co.edu.unbosque.entity.Pago;
import co.edu.unbosque.entity.PoliticaCancelacion;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.CancelacionRepository;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.service.api.CancelacionServiceAPI;
import co.edu.unbosque.service.api.DisponibilidadServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.service.api.PagoServiceAPI;
import co.edu.unbosque.utils.AccesoDenegadoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.EstrategiaCancelacion;
import co.edu.unbosque.utils.FabricaEstrategiaCancelacion;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.MaquinaEstados;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.ResultadoPago;
import co.edu.unbosque.utils.Utilidad;

/**
 * Cancelación de reservas.
 * HU-33: la política del alojamiento (patrón Estrategia) calcula el porcentaje a devolver.
 * HU-34: se registran los datos de la cancelación. HU-35: se libera el periodo.
 * El reembolso en Stripe se hace fuera de la transacción y con clave de idempotencia propia,
 * así un doble clic no devuelve el dinero dos veces.
 */
@Service
public class CancelacionServiceImpl extends GenericServiceImpl<Cancelacion, Long> implements CancelacionServiceAPI {

    public static final String REEMBOLSO_PENDIENTE = "REEMBOLSO_PENDIENTE";

    private static final Logger logger = Logger.getLogger(CancelacionServiceImpl.class.getName());

    private static final List<String> CANCELABLES = List.of(Constantes.RESERVA_CONFIRMADA,
            Constantes.RESERVA_PENDIENTE_PAGO, Constantes.RESERVA_EN_REVISION);

    @Autowired
    private CancelacionRepository cancelacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AlojamientoRepository alojamientoRepository;

    @Autowired
    private PoliticaCancelacionRepository politicaCancelacionRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PagoServiceAPI pagoServiceAPI;

    @Autowired
    private DisponibilidadServiceAPI disponibilidadServiceAPI;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    private TransactionTemplate transaccion;

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @Override
    public CrudRepository<Cancelacion, Long> getDao() {
        return cancelacionRepository;
    }

    @Override
    public CancelacionResponse simular(Long usuarioId, String rol, Long reservaId) {
        Reserva reserva = validarCancelable(usuarioId, rol, reservaId);
        return calcular(reserva, Constantes.ROL_ADMINISTRADOR.equals(rol));
    }

    @Override
    public CancelacionResponse cancelar(Long usuarioId, String rol, Long reservaId, String motivo) {
        Reserva reserva = validarCancelable(usuarioId, rol, reservaId);
        CancelacionResponse calculo = calcular(reserva, Constantes.ROL_ADMINISTRADOR.equals(rol));

        // 1. Reembolso fuera de la transacción (puede tardar hasta 10 s, ADR-04).
        //    Si la pasarela falla, la cancelación sigue y el reembolso queda pendiente para reintento (HU-33).
        ResultadoPago reembolso = null;
        boolean reembolsoPendiente = false;
        Pago pagoAprobado = pagoServiceAPI.pagoAprobado(reservaId);
        if (pagoAprobado != null && calculo.getValorDevolucion().compareTo(BigDecimal.ZERO) > 0) {
            reembolso = intentarReembolso(pagoAprobado, calculo.getValorDevolucion(), reservaId);
            reembolsoPendiente = reembolso == null;
        }
        final ResultadoPago reembolsoFinal = reembolso;
        final boolean pendienteFinal = reembolsoPendiente;

        // 2. Transacción: estado, pago, cancelación, periodo liberado, auditoría y outbox.
        return transaccion.execute(estado -> {
            Reserva r = reservaRepository.findById(reservaId).orElseThrow();
            MaquinaEstados.validarReserva(r.getEstado(), Constantes.RESERVA_CANCELADA);
            r.setEstado(Constantes.RESERVA_CANCELADA);

            if (reembolsoFinal != null) {
                pagoAprobado.setEstado(Constantes.PAGO_DEVUELTO);
                pagoRepository.save(pagoAprobado);
            }
            r.setRequiereRevision(false);
            reservaRepository.save(r);
            String estadoFinal = pendienteFinal ? REEMBOLSO_PENDIENTE : estadoFinal(calculo);
            Cancelacion cancelacion = new Cancelacion();
            cancelacion.setReservaId(reservaId);
            cancelacion.setUsuarioId(usuarioId);
            cancelacion.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.trim());
            cancelacion.setValorDevolucion(calculo.getValorDevolucion());
            cancelacion.setEstadoFinal(estadoFinal);
            cancelacion.setFecha(LocalDateTime.now());
            cancelacionRepository.save(cancelacion);

            disponibilidadServiceAPI.liberarPorReserva(reservaId);

            eventoAuditoriaServiceAPI.registrar(usuarioId, "RESERVA", reservaId, "CANCELAR_RESERVA",
                    Constantes.AUDITORIA_EXITO, Utilidad.detalle("politica", calculo.getPolitica(), "motivo", motivo,
                            "estadoFinal", estadoFinal,
                            "porcentaje", calculo.getPorcentajeDevolucion(), "devolucion", calculo.getValorDevolucion(),
                            "referenciaReembolso", reembolsoFinal == null ? null : reembolsoFinal.getReferencia()));
            Alojamiento alojamiento = alojamientoRepository.findById(r.getAlojamientoId()).orElseThrow();
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_CANCELADA, r.getHuespedId(),
                    "Reserva cancelada", "Tu reserva en \"" + alojamiento.getTitulo() + "\" fue cancelada. Devolución: $"
                            + calculo.getValorDevolucion() + " COP.");
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_CANCELADA, alojamiento.getAnfitrionId(),
                    "Reserva cancelada", "Se canceló la reserva " + reservaId + " de \"" + alojamiento.getTitulo()
                            + "\". Las fechas quedaron libres.");

            calculo.setEstadoReserva(Constantes.RESERVA_CANCELADA);
            calculo.setEstadoFinal(estadoFinal);
            calculo.setReferenciaReembolso(reembolsoFinal == null ? null : reembolsoFinal.getReferencia());
            calculo.setFecha(cancelacion.getFecha());
            return calculo;
        });
    }

    private Reserva validarCancelable(Long usuarioId, String rol, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
        boolean esHuesped = reserva.getHuespedId().equals(usuarioId);
        if (!esHuesped && !Constantes.ROL_ADMINISTRADOR.equals(rol)) {
            throw new AccesoDenegadoException("Solo el huésped de la reserva puede cancelarla.");
        }
        if (!CANCELABLES.contains(reserva.getEstado())) {
            throw new ConflictoException("La reserva no se puede cancelar en estado " + reserva.getEstado() + ".");
        }
        if (!LocalDate.now().isBefore(reserva.getFechaLlegada())) {
            throw new ConflictoException("Solo se puede cancelar antes de la fecha de llegada.");
        }
        return reserva;
    }

    /** HU-33: aplica la estrategia de la política del alojamiento. */
    private CancelacionResponse calcular(Reserva reserva, boolean porAdministrador) {
        Alojamiento alojamiento = alojamientoRepository.findById(reserva.getAlojamientoId()).orElseThrow();
        PoliticaCancelacion politica = alojamiento.getPoliticaId() == null ? null
                : politicaCancelacionRepository.findById(alojamiento.getPoliticaId()).orElse(null);
        String tipo = politica == null ? Constantes.POLITICA_FLEXIBLE : politica.getTipo();
        EstrategiaCancelacion estrategia = FabricaEstrategiaCancelacion.obtener(tipo);

        boolean pagada = Constantes.RESERVA_CONFIRMADA.equals(reserva.getEstado());
        BigDecimal totalPagado = pagada ? reserva.getTotal() : BigDecimal.ZERO;
        int porcentaje;
        BigDecimal devolucion;
        if (!pagada) {
            porcentaje = 0;
            devolucion = BigDecimal.ZERO;
        } else if (porAdministrador) {
            // Si cancela el administrador (por ejemplo, publicación bloqueada) el huésped recibe todo.
            porcentaje = 100;
            devolucion = totalPagado;
        } else {
            porcentaje = estrategia.porcentajeDevolucion(LocalDate.now(), reserva.getFechaLlegada());
            devolucion = estrategia.calcularDevolucion(totalPagado, LocalDate.now(), reserva.getFechaLlegada());
        }

        CancelacionResponse respuesta = new CancelacionResponse();
        respuesta.setReservaId(reserva.getId());
        respuesta.setEstadoReserva(reserva.getEstado());
        respuesta.setPolitica(tipo);
        respuesta.setReglaPolitica(porAdministrador ? "Cancelación por el administrador: devolución del 100 %."
                : estrategia.descripcion());
        respuesta.setPorcentajeDevolucion(porcentaje);
        respuesta.setTotalPagado(totalPagado);
        respuesta.setValorDevolucion(devolucion);
        respuesta.setEstadoFinal(estadoFinal(respuesta));
        return respuesta;
    }

    private String estadoFinal(CancelacionResponse calculo) {
        if (calculo.getTotalPagado().compareTo(BigDecimal.ZERO) == 0) {
            return "CANCELADA_SIN_PAGO";
        }
        if (calculo.getPorcentajeDevolucion() >= 100) {
            return "CANCELADA_REEMBOLSO_TOTAL";
        }
        if (calculo.getPorcentajeDevolucion() > 0) {
            return "CANCELADA_REEMBOLSO_PARCIAL";
        }
        return "CANCELADA_SIN_REEMBOLSO";
    }

    /** Devuelve el resultado aprobado o null si la pasarela falló o rechazó (queda pendiente). */
    private ResultadoPago intentarReembolso(Pago pago, BigDecimal monto, Long reservaId) {
        try {
            ResultadoPago resultado = pagoServiceAPI.reembolsar(pago, monto, "reembolso-reserva-" + reservaId);
            if (resultado.isAprobado()) {
                return resultado;
            }
            logger.warning("reembolso_rechazado reserva=" + reservaId + " motivo=\"" + resultado.getMensaje() + "\"");
        } catch (ServicioNoDisponibleException e) {
            logger.warning("reembolso_pendiente reserva=" + reservaId + " motivo=\"" + e.getMessage() + "\"");
        }
        return null;
    }

    /**
     * HU-33: reintenta los reembolsos pendientes con la misma clave de idempotencia,
     * así la pasarela nunca devuelve dos veces el mismo dinero.
     */
    @Override
    public int reintentarReembolsosPendientes() {
        int completados = 0;
        for (Cancelacion cancelacion : cancelacionRepository.findByEstadoFinal(REEMBOLSO_PENDIENTE)) {
            Long reservaId = cancelacion.getReservaId();
            Pago pago = pagoServiceAPI.pagoAprobado(reservaId);
            ResultadoPago resultado = pago == null ? null
                    : intentarReembolso(pago, cancelacion.getValorDevolucion(), reservaId);
            if (pago != null && resultado == null) {
                continue;
            }
            transaccion.executeWithoutResult(estado -> {
                if (pago != null) {
                    pago.setEstado(Constantes.PAGO_DEVUELTO);
                    pagoRepository.save(pago);
                }
                boolean total = pago == null || cancelacion.getValorDevolucion().compareTo(pago.getMonto()) >= 0;
                cancelacion.setEstadoFinal(total ? "CANCELADA_REEMBOLSO_TOTAL" : "CANCELADA_REEMBOLSO_PARCIAL");
                cancelacionRepository.save(cancelacion);
                eventoAuditoriaServiceAPI.registrar(cancelacion.getUsuarioId(), "RESERVA", reservaId,
                        "REEMBOLSO_COMPLETADO", Constantes.AUDITORIA_EXITO, Utilidad.detalle("devolucion",
                                cancelacion.getValorDevolucion(), "referencia", resultado == null ? null
                                        : resultado.getReferencia()));
                reservaRepository.findById(reservaId).ifPresent(r -> eventoOutboxServiceAPI.publicar(
                        Constantes.EVENTO_RESERVA_CANCELADA, r.getHuespedId(), "Reembolso realizado",
                        "Se completó la devolución de $" + cancelacion.getValorDevolucion() + " COP de tu reserva "
                                + reservaId + "."));
            });
            completados++;
        }
        return completados;
    }
}
