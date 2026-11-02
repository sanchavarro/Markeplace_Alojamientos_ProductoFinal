package co.edu.unbosque.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.CargaNotificacion;
import co.edu.unbosque.entity.EventoOutbox;
import co.edu.unbosque.repository.EventoOutboxRepository;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.Utilidad;

@Service
public class EventoOutboxServiceImpl implements EventoOutboxServiceAPI {

    @Autowired
    private EventoOutboxRepository eventoOutboxRepository;

    /** Evento sin enlace. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(String tipo, Long usuarioId, String titulo, String mensaje) {
        publicar(tipo, usuarioId, titulo, mensaje, null);
    }

    /** Se guarda en la transacción del negocio: si la reserva se revierte, el evento también. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(String tipo, Long usuarioId, String titulo, String mensaje, String enlace) {
        CargaNotificacion carga = new CargaNotificacion();
        carga.setUsuarioId(usuarioId);
        carga.setTitulo(titulo);
        carga.setMensaje(mensaje);
        carga.setEnlace(enlace);

        EventoOutbox evento = new EventoOutbox();
        evento.setTipo(tipo);
        evento.setCarga(Utilidad.aJson(carga));
        evento.setEstado(Constantes.OUTBOX_PENDIENTE);
        evento.setIntentos(0);
        evento.setCreadoEn(LocalDateTime.now());
        eventoOutboxRepository.save(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoOutbox> pendientes() {
        return eventoOutboxRepository.findTop50ByEstadoOrderByCreadoEnAsc(Constantes.OUTBOX_PENDIENTE);
    }

    @Override
    @Transactional
    public void marcarProcesado(Long eventoId) {
        eventoOutboxRepository.findById(eventoId).ifPresent(e -> {
            e.setEstado(Constantes.OUTBOX_PROCESADO);
            eventoOutboxRepository.save(e);
        });
    }

    @Override
    @Transactional
    public void registrarFallo(Long eventoId, int intentosMaximos) {
        eventoOutboxRepository.findById(eventoId).ifPresent(e -> {
            int intentos = e.getIntentos() + 1;
            e.setIntentos(Math.min(intentos, intentosMaximos));
            if (intentos >= intentosMaximos) {
                e.setEstado(Constantes.OUTBOX_ERROR);
            }
            eventoOutboxRepository.save(e);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPorEstado(String estado) {
        return eventoOutboxRepository.countByEstado(estado);
    }
}
