package co.edu.unbosque.utils;

import java.util.List;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import co.edu.unbosque.dto.CargaNotificacion;
import co.edu.unbosque.entity.EventoOutbox;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;

/**
 * ADR-07 Outbox con 3 reintentos (T7 Degradación controlada).
 * Cada pocos segundos lee los eventos PENDIENTES y avisa a todos los observadores suscritos.
 * Si algo falla, el evento se reintenta; al tercer fallo queda en ERROR.
 * Una notificación fallida nunca revierte la reserva.
 */
@Component
public class DespachadorOutbox {

    private static final Logger logger = Logger.getLogger(DespachadorOutbox.class.getName());

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    /** Spring inyecta todos los observadores registrados (NotificadorInterno, NotificadorCorreo...). */
    @Autowired
    private List<ObservadorEvento> observadores;

    @Value("${outbox.intentos.maximos}")
    private int intentosMaximos;

    @Scheduled(fixedDelayString = "${outbox.intervalo.ms}")
    public void despachar() {
        List<EventoOutbox> pendientes = eventoOutboxServiceAPI.pendientes();
        for (EventoOutbox evento : pendientes) {
            try {
                CargaNotificacion carga = Utilidad.desdeJson(evento.getCarga(), CargaNotificacion.class);
                for (ObservadorEvento observador : observadores) {
                    observador.notificar(evento.getTipo(), carga);
                }
                eventoOutboxServiceAPI.marcarProcesado(evento.getId());
            } catch (Exception e) {
                logger.warning("outbox_fallo evento=" + evento.getId() + " intento=" + (evento.getIntentos() + 1)
                        + " motivo=\"" + e.getMessage() + "\"");
                eventoOutboxServiceAPI.registrarFallo(evento.getId(), intentosMaximos);
            }
        }
    }
}
