package co.edu.unbosque.service.impl;

import co.edu.unbosque.utils.ReglaNegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.EventoAuditoria;
import co.edu.unbosque.repository.EventoAuditoriaRepository;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.utils.Utilidad;

@Service
public class EventoAuditoriaServiceImpl implements EventoAuditoriaServiceAPI {

    @Autowired
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    /** Propagation.MANDATORY: falla si no hay transacción, así la auditoría siempre va junto con la operación. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Long usuarioId, String entidad, Long entidadId, String accion, String resultado, String detalle) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setUsuarioId(usuarioId);
        evento.setEntidad(entidad);
        evento.setEntidadId(entidadId);
        evento.setAccion(accion);
        evento.setResultado(resultado);
        evento.setDetalle(detalle == null ? "{}" : detalle);
        evento.setFecha(LocalDateTime.now());
        eventoAuditoriaRepository.save(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<EventoAuditoria> consultar(String entidad, Long usuarioId, String accion, LocalDate desde,
                                                     LocalDate hasta, Integer pagina, Integer tamano) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha 'hasta' no puede ser anterior a 'desde'.");
        }
        PageRequest page = PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
        Page<EventoAuditoria> resultado = eventoAuditoriaRepository.filtrar(
                entidad == null ? "" : entidad.trim().toUpperCase(),
                usuarioId == null ? 0L : usuarioId,
                accion == null ? "" : accion.trim().toUpperCase(),
                desde == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : desde.atStartOfDay(),
                hasta == null ? LocalDateTime.of(2100, 1, 1, 0, 0) : hasta.plusDays(1).atStartOfDay(),
                page);
        return PaginaResponse.desde(resultado);
    }
}
