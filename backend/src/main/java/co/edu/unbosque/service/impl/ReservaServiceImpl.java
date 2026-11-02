package co.edu.unbosque.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import co.edu.unbosque.dto.CotizacionResponse;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.ReservaRequest;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.ClaveIdempotencia;
import co.edu.unbosque.entity.Pago;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.entity.ReservaServicioAdicional;
import co.edu.unbosque.entity.ServicioAdicional;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.ClaveIdempotenciaRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.repository.ReservaServicioAdicionalRepository;
import co.edu.unbosque.service.api.CotizacionServiceAPI;
import co.edu.unbosque.service.api.DisponibilidadServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.service.api.PagoServiceAPI;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.utils.AccesoDenegadoException;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.MaquinaEstados;
import co.edu.unbosque.utils.MetricasSistema;
import co.edu.unbosque.utils.PagoRechazadoException;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.ResultadoPago;
import co.edu.unbosque.utils.ServicioNoDisponibleException;
import co.edu.unbosque.utils.Utilidad;

/**
 * Reserva inmediata con pago (flujo principal del sistema).
 *
 * 1. Idempotencia (ADR-03): si la Idempotency-Key ya se procesó, se devuelve la misma respuesta sin cobrar.
 * 2. Transacción 1 (ADR-02): bloqueo pesimista del alojamiento, verificación de disponibilidad,
 *    reserva en PENDIENTE_PAGO, bloqueo automático del periodo, auditoría. Se confirma y se suelta el candado.
 * 3. Pago fuera de la transacción (ADR-04): adaptador de Stripe con tiempo límite de 10 s y cortacircuitos.
 * 4. Transacción 2: CONFIRMADA + pago APROBADO, o PAGO_FALLIDO + periodo liberado. Auditoría y outbox (ADR-06, ADR-07).
 */
@Service
public class ReservaServiceImpl extends GenericServiceImpl<Reserva, Long> implements ReservaServiceAPI {

    private static final Logger logger = Logger.getLogger(ReservaServiceImpl.class.getName());

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AlojamientoRepository alojamientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClaveIdempotenciaRepository claveIdempotenciaRepository;

    @Autowired
    private ReservaServicioAdicionalRepository reservaServicioAdicionalRepository;

    @Autowired
    private CotizacionServiceAPI cotizacionServiceAPI;

    @Autowired
    private DisponibilidadServiceAPI disponibilidadServiceAPI;

    @Autowired
    private PagoServiceAPI pagoServiceAPI;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    @Value("${idempotencia.horas}")
    private int horasIdempotencia;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    private TransactionTemplate transaccion;

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @Override
    public CrudRepository<Reserva, Long> getDao() {
        return reservaRepository;
    }

    // ───────────────────────────── HU-26, HU-27, HU-28: crear reserva ─────────────────────────────

    @Override
    public ReservaResponse reservar(Long huespedId, ReservaRequest request, String claveTexto) {
        UUID clave = parsearClave(claveTexto);
        ReservaResponse previa = respuestaPrevia(clave, huespedId);
        if (previa != null) {
            return previa;
        }
        Reserva reserva;
        try {
            reserva = transaccion.execute(estado -> crearReserva(huespedId, request, clave));
        } catch (DataIntegrityViolationException e) {
            return respuestaPorClaveDuplicada(clave, huespedId);
        }
        if (Constantes.RESERVA_EN_REVISION.equals(reserva.getEstado())) {
            return aResponse(reserva, "Solicitud enviada. El anfitrión debe aprobarla antes del pago.");
        }
        return procesarPago(reserva.getId(), request.getMetodoPago(), clave);
    }

    private Reserva crearReserva(Long huespedId, ReservaRequest request, UUID clave) {
        claveIdempotenciaRepository.insertar(clave, huespedId, LocalDateTime.now().plusHours(horasIdempotencia));

        // ADR-02: SELECT ... FOR UPDATE. Otra reserva del mismo alojamiento espera aquí.
        Alojamiento alojamiento = alojamientoRepository.bloquearPorId(request.getAlojamientoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado."));
        if (!Constantes.ALOJAMIENTO_ACTIVA.equals(alojamiento.getEstado())) {
            throw new ReglaNegocioException("El alojamiento no está publicado para reservas.");
        }
        if (alojamiento.getAnfitrionId().equals(huespedId)) {
            throw new ReglaNegocioException("No puedes reservar tu propio alojamiento.");
        }
        // HU-06: un anfitrión con la cuenta desactivada o restringida no recibe reservas.
        Usuario anfitrion = usuarioRepository.findById(alojamiento.getAnfitrionId()).orElse(null);
        if (anfitrion == null || !Constantes.USUARIO_ACTIVO.equals(anfitrion.getEstado())) {
            throw new ReglaNegocioException("El alojamiento no está publicado para reservas.");
        }
        CotizacionResponse cotizacion = cotizacionServiceAPI.calcular(alojamiento, request.getFechaLlegada(),
                request.getFechaSalida(), request.getNumHuespedes(), request.getServiciosAdicionales());

        // HU-27: revalidar disponibilidad con el candado puesto (sin superposiciones).
        if (!disponibilidadServiceAPI.estaDisponible(alojamiento.getId(), request.getFechaLlegada(),
                request.getFechaSalida())) {
            MetricasSistema.getInstancia().registrarConflictoReserva();
            throw new ConflictoException("Las fechas ya no están disponibles para este alojamiento.");
        }

        boolean inmediata = Constantes.MODO_INMEDIATA.equals(alojamiento.getModoReserva());
        Reserva reserva = Reserva.builder()
                .huesped(huespedId)
                .alojamiento(alojamiento.getId())
                .fechas(request.getFechaLlegada(), request.getFechaSalida())
                .numHuespedes(request.getNumHuespedes())
                .noches(cotizacion.getNoches())
                .precioBase(cotizacion.getPrecioBase())
                .tarifaLimpieza(cotizacion.getTarifaLimpieza())
                .tarifaServicio(cotizacion.getTarifaServicio())
                .descuento(cotizacion.getDescuento())
                .total(cotizacion.getTotal())
                .estado(inmediata ? Constantes.RESERVA_PENDIENTE_PAGO : Constantes.RESERVA_EN_REVISION)
                .build();
        reserva = reservaRepository.save(reserva);

        for (ServicioAdicional servicio : cotizacion.getDetalleServicios()) {
            reservaServicioAdicionalRepository.save(
                    new ReservaServicioAdicional(reserva.getId(), servicio.getId(), servicio.getPrecio()));
        }
        // HU-18: bloqueo automático del periodo reservado.
        disponibilidadServiceAPI.bloquearPorReserva(alojamiento.getId(), reserva.getId(),
                reserva.getFechaLlegada(), reserva.getFechaSalida());

        eventoAuditoriaServiceAPI.registrar(huespedId, "RESERVA", reserva.getId(), "CREAR_RESERVA",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("alojamientoId", alojamiento.getId(),
                        "llegada", reserva.getFechaLlegada().toString(), "salida", reserva.getFechaSalida().toString(),
                        "total", reserva.getTotal(), "estado", reserva.getEstado(), "idempotencyKey", clave.toString()));

        ClaveIdempotencia registro = claveIdempotenciaRepository.findById(clave).orElseThrow();
        registro.setReservaId(reserva.getId());
        if (!inmediata) {
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_POR_APROBAR, alojamiento.getAnfitrionId(),
                    "Nueva solicitud de reserva", "Tienes una solicitud para \"" + alojamiento.getTitulo()
                            + "\" del " + reserva.getFechaLlegada() + " al " + reserva.getFechaSalida() + ".");
            registro.setRespuesta(Utilidad.aJson(aResponse(reserva, alojamiento.getTitulo(), null, null,
                    "Solicitud enviada. El anfitrión debe aprobarla antes del pago.")));
        }
        claveIdempotenciaRepository.save(registro);
        return reserva;
    }

    // ───────────────────────────── HU-31: pago (modo aprobación) ─────────────────────────────

    @Override
    public ReservaResponse pagar(Long huespedId, Long reservaId, String metodoPago, String claveTexto) {
        UUID clave = parsearClave(claveTexto);
        ReservaResponse previa = respuestaPrevia(clave, huespedId);
        if (previa != null) {
            return previa;
        }
        try {
            transaccion.executeWithoutResult(estado -> {
                claveIdempotenciaRepository.insertar(clave, huespedId, LocalDateTime.now().plusHours(horasIdempotencia));
                Reserva reserva = obtener(reservaId);
                if (!reserva.getHuespedId().equals(huespedId)) {
                    throw new AccesoDenegadoException("Esta reserva no te pertenece.");
                }
                if (!Constantes.RESERVA_PENDIENTE_PAGO.equals(reserva.getEstado())) {
                    throw new ReglaNegocioException("La reserva no está pendiente de pago (estado: "
                            + reserva.getEstado() + ").");
                }
                if (pagoServiceAPI.pagoAprobado(reservaId) != null) {
                    throw new ConflictoException("Esta reserva ya tiene un pago aprobado.");
                }
                ClaveIdempotencia registro = claveIdempotenciaRepository.findById(clave).orElseThrow();
                registro.setReservaId(reservaId);
                claveIdempotenciaRepository.save(registro);
            });
        } catch (DataIntegrityViolationException e) {
            return respuestaPorClaveDuplicada(clave, huespedId);
        }
        return procesarPago(reservaId, metodoPago, clave);
    }

    /** ADR-04: el cobro ocurre sin transacción abierta; luego se guarda el resultado en una transacción corta. */
    private ReservaResponse procesarPago(Long reservaId, String metodoPago, UUID clave) {
        Reserva reserva = obtener(reservaId);
        Alojamiento alojamiento = alojamientoRepository.findById(reserva.getAlojamientoId()).orElseThrow();
        ResultadoPago resultado = null;
        String motivo = null;
        boolean pasarelaCaida = false;
        try {
            resultado = pagoServiceAPI.cobrar(reserva.getTotal(), metodoPago, clave,
                    "Reserva " + reserva.getId() + " - " + alojamiento.getTitulo());
        } catch (PagoRechazadoException e) {
            motivo = e.getMessage();
        } catch (ServicioNoDisponibleException e) {
            motivo = e.getMessage();
            pasarelaCaida = true;
        }
        final ResultadoPago resultadoFinal = resultado;
        final String motivoFinal = motivo;
        final boolean caida = pasarelaCaida;
        return transaccion.execute(estado -> finalizarPago(reservaId, clave, resultadoFinal, motivoFinal, caida));
    }

    private ReservaResponse finalizarPago(Long reservaId, UUID clave, ResultadoPago resultado, String motivo,
                                          boolean pasarelaCaida) {
        Reserva reserva = obtener(reservaId);
        Alojamiento alojamiento = alojamientoRepository.findById(reserva.getAlojamientoId()).orElseThrow();
        ReservaResponse respuesta;
        if (resultado != null && resultado.isAprobado()) {
            MaquinaEstados.validarReserva(reserva.getEstado(), Constantes.RESERVA_CONFIRMADA);
            reserva.setEstado(Constantes.RESERVA_CONFIRMADA);
            reservaRepository.save(reserva);
            pagoServiceAPI.registrar(reserva.getId(), reserva.getTotal(), Constantes.PAGO_APROBADO,
                    resultado.getReferencia(), clave);
            eventoAuditoriaServiceAPI.registrar(reserva.getHuespedId(), "RESERVA", reserva.getId(), "PAGO_APROBADO",
                    Constantes.AUDITORIA_EXITO, Utilidad.detalle("referencia", resultado.getReferencia(),
                            "total", reserva.getTotal()));
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_CONFIRMADA, reserva.getHuespedId(),
                    "Reserva confirmada", "Tu reserva en \"" + alojamiento.getTitulo() + "\" del "
                            + reserva.getFechaLlegada() + " al " + reserva.getFechaSalida() + " está confirmada.");
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_CONFIRMADA, alojamiento.getAnfitrionId(),
                    "Nueva reserva", "Recibiste una reserva confirmada en \"" + alojamiento.getTitulo() + "\".");
            MetricasSistema.getInstancia().registrarReservaConfirmada();
            respuesta = aResponse(reserva, alojamiento.getTitulo(), Constantes.PAGO_APROBADO, resultado.getReferencia(),
                    "Reserva confirmada y pago aprobado.");
        } else {
            MaquinaEstados.validarReserva(reserva.getEstado(), Constantes.RESERVA_PAGO_FALLIDO);
            reserva.setEstado(Constantes.RESERVA_PAGO_FALLIDO);
            reservaRepository.save(reserva);
            disponibilidadServiceAPI.liberarPorReserva(reserva.getId());
            pagoServiceAPI.registrar(reserva.getId(), reserva.getTotal(), Constantes.PAGO_RECHAZADO, null, clave);
            eventoAuditoriaServiceAPI.registrar(reserva.getHuespedId(), "RESERVA", reserva.getId(), "PAGO_FALLIDO",
                    Constantes.AUDITORIA_FALLO, Utilidad.detalle("motivo", motivo, "pasarelaCaida", pasarelaCaida));
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_PAGO_FALLIDO, reserva.getHuespedId(),
                    "Pago no realizado", "No se pudo cobrar la reserva en \"" + alojamiento.getTitulo()
                            + "\". Las fechas quedaron libres.");
            MetricasSistema.getInstancia().registrarPagoRechazado();
            respuesta = aResponse(reserva, alojamiento.getTitulo(),
                    pasarelaCaida ? Constantes.PAGO_NO_DISPONIBLE : Constantes.PAGO_RECHAZADO, null, motivo);
        }
        claveIdempotenciaRepository.findById(clave).ifPresent(registro -> {
            registro.setRespuesta(Utilidad.aJson(respuesta));
            claveIdempotenciaRepository.save(registro);
        });
        logger.info("reserva=" + reserva.getId() + " estado=" + reserva.getEstado() + " estadoPago="
                + respuesta.getEstadoPago());
        return respuesta;
    }

    // ───────────────────────────── HU-14: aprobación del anfitrión ─────────────────────────────

    @Override
    @Transactional
    public ReservaResponse aprobar(Long anfitrionId, Long reservaId) {
        Reserva reserva = reservaDelAnfitrion(anfitrionId, reservaId);
        MaquinaEstados.validarReserva(reserva.getEstado(), Constantes.RESERVA_PENDIENTE_PAGO);
        reserva.setEstado(Constantes.RESERVA_PENDIENTE_PAGO);
        reservaRepository.save(reserva);
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "RESERVA", reservaId, "APROBAR_RESERVA",
                Constantes.AUDITORIA_EXITO, null);
        eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_APROBADA, reserva.getHuespedId(),
                "Reserva aprobada", "El anfitrión aprobó tu solicitud. Realiza el pago para confirmarla.");
        return aResponse(reserva, "Reserva aprobada. El huésped debe realizar el pago.");
    }

    @Override
    @Transactional
    public ReservaResponse rechazar(Long anfitrionId, Long reservaId) {
        Reserva reserva = reservaDelAnfitrion(anfitrionId, reservaId);
        if (!Constantes.RESERVA_EN_REVISION.equals(reserva.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden rechazar solicitudes EN_REVISION.");
        }
        reserva.setEstado(Constantes.RESERVA_CANCELADA);
        reservaRepository.save(reserva);
        disponibilidadServiceAPI.liberarPorReserva(reservaId);
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "RESERVA", reservaId, "RECHAZAR_RESERVA",
                Constantes.AUDITORIA_EXITO, null);
        eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_CANCELADA, reserva.getHuespedId(),
                "Solicitud rechazada", "El anfitrión no aprobó tu solicitud de reserva.");
        return aResponse(reserva, "Solicitud rechazada y fechas liberadas.");
    }

    private Reserva reservaDelAnfitrion(Long anfitrionId, Long reservaId) {
        Reserva reserva = obtener(reservaId);
        Alojamiento alojamiento = alojamientoRepository.findById(reserva.getAlojamientoId()).orElseThrow();
        if (!alojamiento.getAnfitrionId().equals(anfitrionId)) {
            throw new AccesoDenegadoException("La reserva no es de uno de tus alojamientos.");
        }
        return reserva;
    }

    // ───────────────────────────── HU-29, HU-30: consultas ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> historialHuesped(Long huespedId, Integer pagina, Integer tamano) {
        return PaginaResponse.desde(reservaRepository.findByHuespedIdOrderByCreadaEnDesc(huespedId,
                pagina(pagina, tamano)), r -> aResponse(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> reservasAnfitrion(Long anfitrionId, Long alojamientoId, Integer pagina,
                                                             Integer tamano) {
        return PaginaResponse.desde(reservaRepository.buscarPorAnfitrion(anfitrionId,
                alojamientoId == null ? 0L : alojamientoId, pagina(pagina, tamano)), r -> aResponse(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> enRevisionManual(Integer pagina, Integer tamano) {
        return PaginaResponse.desde(reservaRepository.findByRequiereRevisionTrueOrderByFechaLlegadaAsc(
                pagina(pagina, tamano)), r -> aResponse(r, null));
    }

    @Override
    @Transactional
    public ReservaResponse mantenerTrasRevision(Long adminId, Long reservaId, String motivo) {
        Reserva reserva = obtener(reservaId);
        if (!reserva.getRequiereRevision()) {
            throw new ReglaNegocioException("Esta reserva no está en revisión manual.");
        }
        reserva.setRequiereRevision(false);
        reservaRepository.save(reserva);
        eventoAuditoriaServiceAPI.registrar(adminId, "RESERVA", reservaId, "MANTENER_RESERVA_REVISADA",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("motivo", motivo));
        eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RESERVA_CONFIRMADA, reserva.getHuespedId(),
                "Reserva revisada", "El administrador revisó tu reserva " + reservaId + " y se mantiene.");
        return aResponse(reserva, "La reserva se mantiene.");
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse detalle(Long usuarioId, String rol, Long reservaId) {
        Reserva reserva = obtener(reservaId);
        boolean esHuesped = reserva.getHuespedId().equals(usuarioId);
        boolean esAnfitrion = alojamientoRepository.findById(reserva.getAlojamientoId())
                .map(a -> a.getAnfitrionId().equals(usuarioId)).orElse(false);
        if (!esHuesped && !esAnfitrion && !Constantes.ROL_ADMINISTRADOR.equals(rol)) {
            throw new AccesoDenegadoException("No tienes acceso a esta reserva.");
        }
        return aResponse(reserva, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Reserva obtener(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
    }

    @Override
    public ReservaResponse aResponse(Reserva reserva, String mensaje) {
        String titulo = alojamientoRepository.findById(reserva.getAlojamientoId())
                .map(Alojamiento::getTitulo).orElse(null);
        Pago pago = pagoServiceAPI.ultimoPago(reserva.getId());
        return aResponse(reserva, titulo, pago == null ? null : pago.getEstado(),
                pago == null ? null : pago.getReferenciaStripe(), mensaje);
    }

    private ReservaResponse aResponse(Reserva r, String titulo, String estadoPago, String referencia, String mensaje) {
        ReservaResponse dto = new ReservaResponse();
        dto.setId(r.getId());
        dto.setAlojamientoId(r.getAlojamientoId());
        dto.setTituloAlojamiento(titulo);
        dto.setHuespedId(r.getHuespedId());
        dto.setHuespedNombre(usuarioRepository.findById(r.getHuespedId()).map(Usuario::getNombre).orElse(null));
        dto.setRequiereRevision(r.getRequiereRevision());
        dto.setFechaLlegada(r.getFechaLlegada());
        dto.setFechaSalida(r.getFechaSalida());
        dto.setNumHuespedes(r.getNumHuespedes());
        dto.setNoches(r.getNoches());
        dto.setPrecioBase(r.getPrecioBase());
        dto.setTarifaLimpieza(r.getTarifaLimpieza());
        dto.setTarifaServicio(r.getTarifaServicio());
        dto.setDescuento(r.getDescuento());
        dto.setTotal(r.getTotal());
        dto.setEstado(r.getEstado());
        dto.setEstadoPago(estadoPago);
        dto.setReferenciaPago(referencia);
        dto.setMensaje(mensaje);
        dto.setCreadaEn(r.getCreadaEn());
        return dto;
    }

    // ───────────────────────────── Idempotencia (ADR-03) ─────────────────────────────

    private UUID parsearClave(String claveTexto) {
        if (claveTexto == null || claveTexto.isBlank()) {
            throw new IllegalArgumentException("El encabezado Idempotency-Key es obligatorio (un UUID).");
        }
        try {
            return UUID.fromString(claveTexto.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Idempotency-Key debe ser un UUID válido.");
        }
    }

    private ReservaResponse respuestaPrevia(UUID clave, Long usuarioId) {
        Optional<ClaveIdempotencia> encontrada = claveIdempotenciaRepository.findById(clave);
        if (encontrada.isEmpty()) {
            return null;
        }
        ClaveIdempotencia registro = encontrada.get();
        if (!registro.getUsuarioId().equals(usuarioId)) {
            throw new ConflictoException("Esta Idempotency-Key ya fue usada por otra solicitud.");
        }
        if (registro.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ConflictoException("La Idempotency-Key expiró. Genera una nueva para un nuevo intento.");
        }
        if (registro.getRespuesta() == null) {
            throw new ConflictoException("Ya hay una solicitud en proceso con esta Idempotency-Key.");
        }
        MetricasSistema.getInstancia().registrarRespuestaIdempotente();
        ReservaResponse respuesta = Utilidad.desdeJson(registro.getRespuesta(), ReservaResponse.class);
        respuesta.setMensaje(respuesta.getMensaje() + " (respuesta repetida por Idempotency-Key: no se volvió a cobrar)");
        return respuesta;
    }

    private ReservaResponse respuestaPorClaveDuplicada(UUID clave, Long usuarioId) {
        ReservaResponse otra = respuestaPrevia(clave, usuarioId);
        if (otra != null) {
            return otra;
        }
        throw new ConflictoException("Ya hay una solicitud en proceso con esta Idempotency-Key.");
    }

    // ───────────────────────────── Tareas programadas ─────────────────────────────

    /** Las reservas confirmadas cuya fecha de salida ya llegó pasan a COMPLETADA (habilita reseñas, HU-38). */
    @Override
    @Transactional
    public int completarReservasFinalizadas() {
        List<Reserva> finalizadas = reservaRepository.findByEstadoAndFechaSalidaLessThanEqual(
                Constantes.RESERVA_CONFIRMADA, LocalDate.now());
        for (Reserva reserva : finalizadas) {
            reserva.setEstado(Constantes.RESERVA_COMPLETADA);
            reservaRepository.save(reserva);
            eventoAuditoriaServiceAPI.registrar(null, "RESERVA", reserva.getId(), "COMPLETAR_RESERVA",
                    Constantes.AUDITORIA_EXITO, null);
        }
        return finalizadas.size();
    }

    /** Reservas que se quedaron en PENDIENTE_PAGO más de 24 h se dan por fallidas y se liberan las fechas. */
    @Override
    @Transactional
    public int vencerPagosPendientes() {
        List<Reserva> vencidas = reservaRepository.findByEstadoAndCreadaEnBefore(Constantes.RESERVA_PENDIENTE_PAGO,
                LocalDateTime.now().minusHours(horasIdempotencia));
        for (Reserva reserva : vencidas) {
            reserva.setEstado(Constantes.RESERVA_PAGO_FALLIDO);
            reservaRepository.save(reserva);
            disponibilidadServiceAPI.liberarPorReserva(reserva.getId());
            eventoAuditoriaServiceAPI.registrar(null, "RESERVA", reserva.getId(), "VENCER_PAGO_PENDIENTE",
                    Constantes.AUDITORIA_EXITO, null);
        }
        return vencidas.size();
    }

    @Override
    @Transactional
    public int limpiarClavesIdempotencia() {
        return claveIdempotenciaRepository.eliminarExpiradas(LocalDateTime.now());
    }

    private PageRequest pagina(Integer pagina, Integer tamano) {
        return PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
    }
}
