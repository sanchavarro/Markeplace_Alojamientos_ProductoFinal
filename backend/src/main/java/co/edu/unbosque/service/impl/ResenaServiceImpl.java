package co.edu.unbosque.service.impl;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.ResenaRequest;
import co.edu.unbosque.dto.ResenasAlojamientoResponse;
import co.edu.unbosque.entity.Resena;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.repository.ResenaRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.ResenaServiceAPI;
import co.edu.unbosque.utils.AccesoDenegadoException;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class ResenaServiceImpl extends GenericServiceImpl<Resena, Long> implements ResenaServiceAPI {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    @Override
    public CrudRepository<Resena, Long> getDao() {
        return resenaRepository;
    }

    /**
     * HU-38: solo el huésped de una reserva COMPLETADA puede reseñarla, una sola vez.
     * HU-39: la reseña conserva la relación con la reserva, el huésped y el alojamiento.
     */
    @Override
    @Transactional
    public Resena crear(Long huespedId, ResenaRequest request) {
        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
        if (!reserva.getHuespedId().equals(huespedId)) {
            throw new AccesoDenegadoException("Solo puedes reseñar tus propias reservas.");
        }
        if (!Constantes.RESERVA_COMPLETADA.equals(reserva.getEstado())) {
            throw new ReglaNegocioException("Solo se puede reseñar una estadía COMPLETADA.");
        }
        if (resenaRepository.existsByReservaId(reserva.getId())) {
            throw new ConflictoException("Ya registraste una reseña para esta reserva.");
        }
        Resena resena = new Resena();
        resena.setReservaId(reserva.getId());
        resena.setHuespedId(huespedId);
        resena.setAlojamientoId(reserva.getAlojamientoId());
        resena.setCalificacion(request.getCalificacion());
        resena.setComentario(request.getComentario() == null ? null : request.getComentario().trim());
        resena.setVisible(true);
        resena.setCreadaEn(LocalDateTime.now());
        resena = resenaRepository.save(resena);
        eventoAuditoriaServiceAPI.registrar(huespedId, "RESENA", resena.getId(), "CREAR_RESENA",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("calificacion", resena.getCalificacion()));
        return resena;
    }

    /** HU-40: reseñas visibles y promedio. */
    @Override
    @Transactional(readOnly = true)
    public ResenasAlojamientoResponse listar(Long alojamientoId, Integer pagina, Integer tamano) {
        PageRequest page = PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
        Double promedio = resenaRepository.promedioPorAlojamiento(alojamientoId);
        ResenasAlojamientoResponse respuesta = new ResenasAlojamientoResponse();
        respuesta.setAlojamientoId(alojamientoId);
        respuesta.setPromedio(promedio == null ? null : Math.round(promedio * 10.0) / 10.0);
        respuesta.setTotal(resenaRepository.countByAlojamientoIdAndVisibleTrue(alojamientoId));
        respuesta.setResenas(PaginaResponse.desde(
                resenaRepository.findByAlojamientoIdAndVisibleTrueOrderByCreadaEnDesc(alojamientoId, page)));
        return respuesta;
    }

    /** HU-41: el administrador oculta una reseña (no se borra, para conservar la trazabilidad). */
    @Override
    @Transactional
    public Resena cambiarVisibilidad(Long adminId, Long resenaId, boolean visible, String motivo) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada."));
        resena.setVisible(visible);
        resenaRepository.save(resena);
        eventoAuditoriaServiceAPI.registrar(adminId, "RESENA", resenaId, visible ? "MOSTRAR_RESENA" : "OCULTAR_RESENA",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("motivo", motivo));
        return resena;
    }
}
