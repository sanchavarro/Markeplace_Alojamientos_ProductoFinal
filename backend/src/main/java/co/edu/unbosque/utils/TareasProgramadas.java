package co.edu.unbosque.utils;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import co.edu.unbosque.service.api.CancelacionServiceAPI;
import co.edu.unbosque.service.api.ReporteServiceAPI;
import co.edu.unbosque.service.api.ReservaServiceAPI;

/**
 * Tareas en segundo plano (ADR-08): completar estadías terminadas, vencer pagos pendientes
 * limpiar claves de idempotencia de más de 24 h (ADR-03) y reintentar reembolsos pendientes (HU-33).
 */
@Component
public class TareasProgramadas {

    private static final Logger logger = Logger.getLogger(TareasProgramadas.class.getName());

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    @Autowired
    private CancelacionServiceAPI cancelacionServiceAPI;

    @Autowired
    private ReporteServiceAPI reporteServiceAPI;

    /** Todos los días a las 00:05. */
    @Scheduled(cron = "0 5 0 * * *")
    public void completarReservas() {
        int total = reservaServiceAPI.completarReservasFinalizadas();
        logger.info("tarea=completar_reservas total=" + total);
    }

    /** Cada hora. */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void mantenimiento() {
        int vencidas = reservaServiceAPI.vencerPagosPendientes();
        int claves = reservaServiceAPI.limpiarClavesIdempotencia();
        logger.info("tarea=mantenimiento pagos_vencidos=" + vencidas + " claves_eliminadas=" + claves);
    }

    /** Cada 10 minutos: reembolsos que fallaron por la pasarela (misma clave de idempotencia). */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000)
    public void reintentarReembolsos() {
        int completados = cancelacionServiceAPI.reintentarReembolsosPendientes();
        if (completados > 0) {
            logger.info("tarea=reintentar_reembolsos completados=" + completados);
        }
    }

    /** Cada 5 minutos: exportaciones de reportes que quedaron pendientes (HU-47). */
    @Scheduled(fixedDelay = 300000, initialDelay = 180000)
    public void retomarReportes() {
        int retomadas = reporteServiceAPI.retomarExportacionesPendientes();
        if (retomadas > 0) {
            logger.info("tarea=retomar_reportes total=" + retomadas);
        }
    }
}
