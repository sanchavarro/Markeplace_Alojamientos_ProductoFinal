package co.edu.unbosque.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.ReporteExportacion;
import co.edu.unbosque.repository.ReporteExportacionRepository;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.GeneradorExcel;
import co.edu.unbosque.utils.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.ReporteServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GeneradorCsv;
import co.edu.unbosque.utils.GeneradorPdf;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class ReporteServiceImpl implements ReporteServiceAPI {

    private static final Logger logger = Logger.getLogger(ReporteServiceImpl.class.getName());

    private static final String TIPO_RESERVAS = "RESERVAS";
    private static final String FORMATO_CSV = "CSV";
    private static final String FORMATO_PDF = "PDF";
    private static final String FORMATO_EXCEL = "EXCEL";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_PROCESANDO = "PROCESANDO";
    private static final String ESTADO_LISTO = "LISTO";
    private static final String ESTADO_ERROR = "ERROR";

    /** Un solo hilo con prioridad mínima: los reportes no compiten con las búsquedas (RNF-03). */
    private final ExecutorService ejecutorReportes = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "reportes");
        hilo.setPriority(Thread.MIN_PRIORITY);
        hilo.setDaemon(true);
        return hilo;
    });

    @Autowired
    private ReporteExportacionRepository reporteExportacionRepository;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    private TransactionTemplate transaccion;

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlojamientoRepository alojamientoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    /** HU-46: indicadores principales para el administrador. */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> datos = new LinkedHashMap<>();

        Map<String, Long> usuarios = new LinkedHashMap<>();
        usuarios.put("huespedes", usuarioRepository.countByRol(Constantes.ROL_HUESPED));
        usuarios.put("anfitriones", usuarioRepository.countByRol(Constantes.ROL_ANFITRION));
        usuarios.put("restringidos", usuarioRepository.countByEstado(Constantes.USUARIO_RESTRINGIDO));
        datos.put("usuarios", usuarios);

        Map<String, Long> publicaciones = new LinkedHashMap<>();
        for (String estado : List.of(Constantes.ALOJAMIENTO_BORRADOR, Constantes.ALOJAMIENTO_PENDIENTE_REVISION,
                Constantes.ALOJAMIENTO_ACTIVA, Constantes.ALOJAMIENTO_PAUSADA, Constantes.ALOJAMIENTO_BLOQUEADA)) {
            publicaciones.put(estado, alojamientoRepository.countByEstado(estado));
        }
        datos.put("publicaciones", publicaciones);

        Map<String, Long> reservas = new LinkedHashMap<>();
        for (String estado : List.of(Constantes.RESERVA_EN_REVISION, Constantes.RESERVA_PENDIENTE_PAGO,
                Constantes.RESERVA_CONFIRMADA, Constantes.RESERVA_PAGO_FALLIDO, Constantes.RESERVA_CANCELADA,
                Constantes.RESERVA_COMPLETADA)) {
            reservas.put(estado, reservaRepository.countByEstado(estado));
        }
        datos.put("reservas", reservas);
        datos.put("ingresosAprobadosCOP", pagoRepository.sumarAprobados());

        // HU-46: ciudades con más demanda (top 5).
        List<Map<String, Object>> ciudades = new ArrayList<>();
        for (Object[] fila : reservaRepository.ciudadesConMasReservas(PageRequest.of(0, 5))) {
            Map<String, Object> ciudad = new LinkedHashMap<>();
            ciudad.put("ciudad", fila[0]);
            ciudad.put("reservas", fila[1]);
            ciudades.add(ciudad);
        }
        datos.put("ciudadesConMasDemanda", ciudades);
        return datos;
    }

    /** HU-47 (descarga inmediata) y HU-48 registrar quién exporta (auditoría en la misma transacción). */
    @Override
    @Transactional
    public byte[] exportarReservas(Long adminId, String formato, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = inicio(desde);
        LocalDate fin = fin(desde, hasta);
        String tipo = validarFormato(formato);
        List<Reserva> lista = consultarReservas(inicio, fin);
        eventoAuditoriaServiceAPI.registrar(adminId, "REPORTE", null, "EXPORTAR_REPORTE", Constantes.AUDITORIA_EXITO,
                Utilidad.detalle("tipo", TIPO_RESERVAS, "formato", tipo, "desde", inicio.toString(),
                        "hasta", fin.toString(), "registros", lista.size()));
        return construirArchivo(tipo, inicio, fin, lista);
    }

    // ─────────────────── HU-47: exportación en segundo plano con menor prioridad (ADR-08 T2) ───────────────────

    /** Guarda la solicitud y, cuando la transacción termina, la genera en un hilo de baja prioridad. */
    @Override
    @Transactional
    public ReporteExportacion solicitarExportacion(Long adminId, String formato, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = inicio(desde);
        LocalDate fin = fin(desde, hasta);
        String tipo = validarFormato(formato);
        ReporteExportacion exportacion = new ReporteExportacion();
        exportacion.setAdminId(adminId);
        exportacion.setTipo(TIPO_RESERVAS);
        exportacion.setFormato(tipo);
        exportacion.setDesde(inicio);
        exportacion.setHasta(fin);
        exportacion.setEstado(ESTADO_PENDIENTE);
        exportacion.setCreadoEn(LocalDateTime.now());
        exportacion = reporteExportacionRepository.save(exportacion);
        // HU-48: quién exporta, cuándo, qué reporte y con qué filtros.
        eventoAuditoriaServiceAPI.registrar(adminId, "REPORTE", exportacion.getId(), "SOLICITAR_EXPORTACION",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("tipo", TIPO_RESERVAS, "formato", tipo,
                        "desde", inicio.toString(), "hasta", fin.toString()));
        final Long id = exportacion.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ejecutorReportes.submit(() -> procesarExportacion(id));
            }
        });
        return exportacion;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReporteExportacion> misExportaciones(Long adminId, Integer pagina, Integer tamano) {
        PageRequest page = PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
        return PaginaResponse.desde(reporteExportacionRepository.findByAdminIdOrderByCreadoEnDesc(adminId, page));
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteExportacion obtenerExportacion(Long id) {
        return reporteExportacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exportación no encontrada."));
    }

    @Override
    @Transactional
    public ReporteExportacion descargarExportacion(Long adminId, Long id) {
        ReporteExportacion exportacion = obtenerExportacion(id);
        if (!ESTADO_LISTO.equals(exportacion.getEstado())) {
            throw new ConflictoException("El reporte todavía no está listo (estado " + exportacion.getEstado() + ").");
        }
        eventoAuditoriaServiceAPI.registrar(adminId, "REPORTE", id, "DESCARGAR_EXPORTACION", Constantes.AUDITORIA_EXITO,
                Utilidad.detalle("formato", exportacion.getFormato(), "archivo", exportacion.getNombreArchivo()));
        return exportacion;
    }

    /** Genera el archivo. Se ejecuta en el hilo "reportes" (prioridad mínima) para no frenar la búsqueda. */
    @Override
    public void procesarExportacion(Long id) {
        ReporteExportacion exportacion = transaccion.execute(estado -> {
            ReporteExportacion e = reporteExportacionRepository.findById(id).orElse(null);
            if (e == null || !ESTADO_PENDIENTE.equals(e.getEstado())) {
                return null;
            }
            e.setEstado(ESTADO_PROCESANDO);
            return reporteExportacionRepository.save(e);
        });
        if (exportacion == null) {
            return;
        }
        try {
            transaccion.executeWithoutResult(estado -> {
                List<Reserva> lista = consultarReservas(exportacion.getDesde(), exportacion.getHasta());
                exportacion.setArchivo(construirArchivo(exportacion.getFormato(), exportacion.getDesde(),
                        exportacion.getHasta(), lista));
                exportacion.setRegistros(lista.size());
                exportacion.setNombreArchivo("reservas_" + exportacion.getDesde() + "_" + exportacion.getHasta() + "."
                        + extension(exportacion.getFormato()));
                exportacion.setEstado(ESTADO_LISTO);
                exportacion.setTerminadoEn(LocalDateTime.now());
                reporteExportacionRepository.save(exportacion);
                eventoOutboxServiceAPI.publicar(Constantes.EVENTO_REPORTE_LISTO, exportacion.getAdminId(),
                        "Reporte listo", "Tu reporte de reservas en " + exportacion.getFormato()
                                + " está listo para descargar (exportación " + id + ").");
            });
            logger.info("reporte_generado id=" + id + " formato=" + exportacion.getFormato());
        } catch (RuntimeException ex) {
            logger.severe("reporte_fallo id=" + id + " motivo=\"" + ex.getMessage() + "\"");
            transaccion.executeWithoutResult(estado -> {
                ReporteExportacion e = reporteExportacionRepository.findById(id).orElseThrow();
                e.setEstado(ESTADO_ERROR);
                e.setMensajeError("No se pudo generar el reporte.");
                e.setTerminadoEn(LocalDateTime.now());
                reporteExportacionRepository.save(e);
                eventoOutboxServiceAPI.publicar(Constantes.EVENTO_REPORTE_LISTO, e.getAdminId(), "Reporte con error",
                        "No se pudo generar la exportación " + id + ". Intenta de nuevo.");
            });
        }
    }

    /** Retoma exportaciones que quedaron PENDIENTES (por ejemplo, si la aplicación se reinició). */
    @Override
    public int retomarExportacionesPendientes() {
        List<ReporteExportacion> pendientes = reporteExportacionRepository.findByEstadoAndCreadoEnBefore(
                ESTADO_PENDIENTE, LocalDateTime.now().minusMinutes(2));
        for (ReporteExportacion e : pendientes) {
            ejecutorReportes.submit(() -> procesarExportacion(e.getId()));
        }
        return pendientes.size();
    }

    @Override
    public String tipoContenido(String formato) {
        switch (formato.toUpperCase()) {
            case FORMATO_PDF:
                return "application/pdf";
            case FORMATO_EXCEL:
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "text/csv; charset=UTF-8";
        }
    }

    @Override
    public String extension(String formato) {
        switch (formato.toUpperCase()) {
            case FORMATO_PDF:
                return "pdf";
            case FORMATO_EXCEL:
                return "xlsx";
            default:
                return "csv";
        }
    }

    // ───────────────────────────── Generación de archivos ─────────────────────────────

    private LocalDate inicio(LocalDate desde) {
        return desde == null ? LocalDate.now().minusMonths(1) : desde;
    }

    private LocalDate fin(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        if (fin.isBefore(inicio(desde))) {
            throw new ReglaNegocioException("La fecha final no puede ser anterior a la inicial.");
        }
        return fin;
    }

    private String validarFormato(String formato) {
        String tipo = formato == null ? FORMATO_CSV : formato.trim().toUpperCase();
        if ("XLSX".equals(tipo)) {
            tipo = FORMATO_EXCEL;
        }
        if (!List.of(FORMATO_CSV, FORMATO_PDF, FORMATO_EXCEL).contains(tipo)) {
            throw new ReglaNegocioException("Formato no válido. Usa csv, pdf o excel.");
        }
        return tipo;
    }

    private List<Reserva> consultarReservas(LocalDate inicio, LocalDate fin) {
        return reservaRepository.findByCreadaEnBetweenOrderByCreadaEnAsc(inicio.atStartOfDay(),
                fin.plusDays(1).atStartOfDay());
    }

    private byte[] construirArchivo(String formato, LocalDate inicio, LocalDate fin, List<Reserva> lista) {
        List<String> encabezados = List.of("id", "alojamiento", "huesped", "llegada", "salida", "noches", "total",
                "estado", "creada");
        List<List<String>> filas = new ArrayList<>();
        for (Reserva r : lista) {
            filas.add(List.of(String.valueOf(r.getId()), String.valueOf(r.getAlojamientoId()),
                    String.valueOf(r.getHuespedId()), r.getFechaLlegada().toString(), r.getFechaSalida().toString(),
                    String.valueOf(r.getNoches()), r.getTotal().toPlainString(), r.getEstado(),
                    r.getCreadaEn().toLocalDate().toString()));
        }
        if (FORMATO_CSV.equals(formato)) {
            return GeneradorCsv.generar(encabezados, filas);
        }
        if (FORMATO_EXCEL.equals(formato)) {
            return GeneradorExcel.generar("Reservas", encabezados, filas);
        }
        List<String> lineas = new ArrayList<>();
        lineas.add("Periodo: " + inicio + " a " + fin + "   Total de reservas: " + lista.size());
        lineas.add("");
        lineas.add(String.format("%-6s %-6s %-6s %-11s %-11s %-4s %-14s %-15s", "ID", "ALOJ", "HUESP", "LLEGADA",
                "SALIDA", "NOCH", "TOTAL COP", "ESTADO"));
        for (Reserva r : lista) {
            lineas.add(String.format("%-6d %-6d %-6d %-11s %-11s %-4d %-14s %-15s", r.getId(), r.getAlojamientoId(),
                    r.getHuespedId(), r.getFechaLlegada(), r.getFechaSalida(), r.getNoches(),
                    r.getTotal().toPlainString(), r.getEstado()));
        }
        return GeneradorPdf.generar("Reporte de reservas - Marketplace Inversiones LR", lineas);
    }
}
