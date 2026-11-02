package co.edu.unbosque.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.Notificacion;
import co.edu.unbosque.repository.NotificacionRepository;
import co.edu.unbosque.service.api.NotificacionServiceAPI;
import co.edu.unbosque.utils.AccesoDenegadoException;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class NotificacionServiceImpl extends GenericServiceImpl<Notificacion, Long> implements NotificacionServiceAPI {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    @Override
    public CrudRepository<Notificacion, Long> getDao() {
        return notificacionRepository;
    }

    @Override
    @Transactional
    public Notificacion crear(Long usuarioId, String tipo, String mensaje) {
        Notificacion n = new Notificacion();
        n.setUsuarioId(usuarioId);
        n.setTipo(tipo.length() > 40 ? tipo.substring(0, 40) : tipo);
        n.setMensaje(mensaje.length() > 255 ? mensaje.substring(0, 252) + "..." : mensaje);
        n.setLeida(false);
        n.setCreadaEn(LocalDateTime.now());
        return notificacionRepository.save(n);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<Notificacion> listar(Long usuarioId, Integer pagina, Integer tamano) {
        PageRequest page = PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
        return PaginaResponse.desde(notificacionRepository.findByUsuarioIdOrderByCreadaEnDesc(usuarioId, page));
    }

    @Override
    @Transactional
    public Notificacion marcarLeida(Long usuarioId, Long notificacionId) {
        Notificacion n = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada."));
        if (!n.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Esta notificación no te pertenece.");
        }
        n.setLeida(true);
        return notificacionRepository.save(n);
    }

    @Override
    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioIdAndLeidaFalse(usuarioId);
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
        return noLeidas.size();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }
}
