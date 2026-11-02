package co.edu.unbosque.service.impl;

import co.edu.unbosque.utils.AlmacenamientoImagenAdapter;
import java.time.LocalDate;
import co.edu.unbosque.entity.Reserva;
import co.edu.unbosque.repository.ReservaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.AlojamientoDTO;
import co.edu.unbosque.dto.AlojamientoRequest;
import co.edu.unbosque.dto.AlojamientoResumenDTO;
import co.edu.unbosque.dto.ImagenRequest;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.ServicioAdicionalRequest;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.AlojamientoServicio;
import co.edu.unbosque.entity.Imagen;
import co.edu.unbosque.entity.Servicio;
import co.edu.unbosque.entity.ServicioAdicional;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.AlojamientoServicioRepository;
import co.edu.unbosque.repository.ImagenRepository;
import co.edu.unbosque.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.repository.ResenaRepository;
import co.edu.unbosque.repository.ServicioAdicionalRepository;
import co.edu.unbosque.repository.ServicioRepository;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.utils.AccesoDenegadoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.MaquinaEstados;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class AlojamientoServiceImpl extends GenericServiceImpl<Alojamiento, Long> implements AlojamientoServiceAPI {

    @Autowired
    private AlojamientoRepository alojamientoRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private ServicioAdicionalRepository servicioAdicionalRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private AlojamientoServicioRepository alojamientoServicioRepository;

    @Autowired
    private PoliticaCancelacionRepository politicaCancelacionRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AlmacenamientoImagenAdapter almacenamientoImagenAdapter;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    @Override
    public CrudRepository<Alojamiento, Long> getDao() {
        return alojamientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Alojamiento obtener(Long id) {
        return alojamientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public Alojamiento obtenerPropio(Long anfitrionId, Long alojamientoId) {
        Alojamiento alojamiento = obtener(alojamientoId);
        if (!alojamiento.getAnfitrionId().equals(anfitrionId)) {
            throw new AccesoDenegadoException("Esta publicación no te pertenece.");
        }
        return alojamiento;
    }

    /** HU-07: la publicación nace en BORRADOR; luego el anfitrión la envía a revisión. */
    @Override
    @Transactional
    public AlojamientoDTO crear(Long anfitrionId, AlojamientoRequest request) {
        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setAnfitrionId(anfitrionId);
        alojamiento.setEstado(Constantes.ALOJAMIENTO_BORRADOR);
        alojamiento.setCreadoEn(LocalDateTime.now());
        aplicarDatos(alojamiento, request);
        alojamiento = alojamientoRepository.save(alojamiento);
        guardarServicios(alojamiento.getId(), request.getServicios());
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "ALOJAMIENTO", alojamiento.getId(), "CREAR_PUBLICACION",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("titulo", alojamiento.getTitulo()));
        return aDTO(alojamiento);
    }

    /**
     * HU-08: edita el dueño o el administrador (RN-01). Las reservas confirmadas guardan su propio precio,
     * así que los cambios no las alteran. El anfitrión no puede editar una publicación bloqueada.
     */
    @Override
    @Transactional
    public AlojamientoDTO editar(Long usuarioId, String rol, Long alojamientoId, AlojamientoRequest request) {
        boolean esAdmin = Constantes.ROL_ADMINISTRADOR.equals(rol);
        Alojamiento alojamiento = esAdmin ? obtener(alojamientoId) : obtenerPropio(usuarioId, alojamientoId);
        if (!esAdmin && Constantes.ALOJAMIENTO_BLOQUEADA.equals(alojamiento.getEstado())) {
            throw new ReglaNegocioException("La publicación está bloqueada por un administrador y no se puede editar.");
        }
        aplicarDatos(alojamiento, request);
        alojamientoRepository.save(alojamiento);
        if (request.getServicios() != null) {
            alojamientoServicioRepository.eliminarPorAlojamiento(alojamientoId);
            guardarServicios(alojamientoId, request.getServicios());
        }
        eventoAuditoriaServiceAPI.registrar(usuarioId, "ALOJAMIENTO", alojamientoId, "EDITAR_PUBLICACION",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("rol", rol));
        return aDTO(alojamiento);
    }

    /** HU-11 a HU-14: validaciones de información mínima, capacidad, precio y modo de reserva. */
    private void aplicarDatos(Alojamiento alojamiento, AlojamientoRequest request) {
        String tipo = request.getTipo().trim().toUpperCase();
        if (!Constantes.TIPOS_ALOJAMIENTO.contains(tipo)) {
            throw new ReglaNegocioException("Tipo de alojamiento no válido. Opciones: " + Constantes.TIPOS_ALOJAMIENTO);
        }
        String modo = request.getModoReserva().trim().toUpperCase();
        if (!Constantes.MODO_INMEDIATA.equals(modo) && !Constantes.MODO_APROBACION.equals(modo)) {
            throw new ReglaNegocioException("El modo de reserva debe ser INMEDIATA o APROBACION.");
        }
        if (request.getCapacidad() == null || request.getCapacidad() <= 0) {
            throw new ReglaNegocioException("La capacidad debe ser mayor a 0.");
        }
        if (request.getPrecioNoche() == null || request.getPrecioNoche().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El precio por noche debe ser mayor a 0.");
        }
        if (!politicaCancelacionRepository.existsById(request.getPoliticaId())) {
            throw new ResourceNotFoundException("La política de cancelación indicada no existe.");
        }
        alojamiento.setTitulo(request.getTitulo().trim());
        alojamiento.setDescripcion(request.getDescripcion().trim());
        alojamiento.setReglas(request.getReglas() == null || request.getReglas().isBlank() ? null : request.getReglas().trim());
        alojamiento.setCiudad(request.getCiudad().trim());
        alojamiento.setDireccion(request.getDireccion().trim());
        alojamiento.setTipo(tipo);
        alojamiento.setCapacidad(request.getCapacidad());
        alojamiento.setPrecioNoche(request.getPrecioNoche());
        alojamiento.setTarifaLimpieza(request.getTarifaLimpieza() == null ? BigDecimal.ZERO : request.getTarifaLimpieza());
        alojamiento.setModoReserva(modo);
        alojamiento.setPoliticaId(request.getPoliticaId());
    }

    private void guardarServicios(Long alojamientoId, List<Long> servicios) {
        if (servicios == null) {
            return;
        }
        for (Long servicioId : new LinkedHashSet<>(servicios)) {
            if (!servicioRepository.existsById(servicioId)) {
                throw new ResourceNotFoundException("El servicio con id " + servicioId + " no existe.");
            }
            alojamientoServicioRepository.save(new AlojamientoServicio(alojamientoId, servicioId));
        }
    }

    /** HU-15: detalle. Solo las ACTIVAS son públicas; el dueño y el administrador ven cualquier estado. */
    @Override
    @Transactional(readOnly = true)
    public AlojamientoDTO detalle(Long alojamientoId, Long usuarioId, String rol) {
        Alojamiento alojamiento = obtener(alojamientoId);
        boolean esDueno = usuarioId != null && usuarioId.equals(alojamiento.getAnfitrionId());
        boolean esAdmin = Constantes.ROL_ADMINISTRADOR.equals(rol);
        if (!Constantes.ALOJAMIENTO_ACTIVA.equals(alojamiento.getEstado()) && !esDueno && !esAdmin) {
            throw new ResourceNotFoundException("Alojamiento no encontrado.");
        }
        return aDTO(alojamiento);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AlojamientoResumenDTO> misAlojamientos(Long anfitrionId, Integer pagina, Integer tamano) {
        return PaginaResponse.desde(alojamientoRepository.findByAnfitrionIdOrderByCreadoEnDesc(anfitrionId,
                pagina(pagina, tamano)), this::aResumen);
    }

    /**
     * HU-09 y HU-10: acciones del anfitrión.
     * ENVIAR_REVISION (BORRADOR → PENDIENTE_REVISION), PAUSAR (ACTIVA → PAUSADA), ACTIVAR (PAUSADA → ACTIVA).
     */
    @Override
    @Transactional
    public AlojamientoDTO cambiarEstadoPorAnfitrion(Long anfitrionId, Long alojamientoId, String accion) {
        Alojamiento alojamiento = obtenerPropio(anfitrionId, alojamientoId);
        String nuevoEstado;
        switch (accion.trim().toUpperCase()) {
            case "ENVIAR_REVISION":
                validarInformacionMinima(alojamiento);
                nuevoEstado = Constantes.ALOJAMIENTO_PENDIENTE_REVISION;
                break;
            case "PAUSAR":
                if (!Constantes.ALOJAMIENTO_ACTIVA.equals(alojamiento.getEstado())) {
                    throw new ReglaNegocioException("Solo puedes pausar una publicación ACTIVA.");
                }
                nuevoEstado = Constantes.ALOJAMIENTO_PAUSADA;
                break;
            case "ACTIVAR":
                if (!Constantes.ALOJAMIENTO_PAUSADA.equals(alojamiento.getEstado())) {
                    throw new ReglaNegocioException("Solo puedes activar una publicación PAUSADA que ya fue aprobada.");
                }
                nuevoEstado = Constantes.ALOJAMIENTO_ACTIVA;
                break;
            default:
                throw new ReglaNegocioException("Acción no válida. Usa ENVIAR_REVISION, PAUSAR o ACTIVAR.");
        }
        return cambiarEstado(alojamiento, nuevoEstado, anfitrionId, "ANFITRION_" + accion.trim().toUpperCase(), null);
    }

    /**
     * HU-42 y HU-43: el administrador aprueba (ACTIVA), pausa o bloquea una publicación.
     * Al bloquear, las reservas futuras se conservan y quedan marcadas para revisión manual.
     */
    @Override
    @Transactional
    public AlojamientoDTO cambiarEstadoPorAdministrador(Long adminId, Long alojamientoId, String nuevoEstado,
                                                        String motivo) {
        Alojamiento alojamiento = obtener(alojamientoId);
        AlojamientoDTO dto = cambiarEstado(alojamiento, nuevoEstado, adminId, "ADMIN_" + nuevoEstado, motivo);
        if (Constantes.ALOJAMIENTO_BLOQUEADA.equals(nuevoEstado)) {
            marcarReservasParaRevision(adminId, alojamiento);
        }
        eventoOutboxServiceAPI.publicar(Constantes.EVENTO_PUBLICACION_ESTADO, alojamiento.getAnfitrionId(),
                "Estado de tu publicación", "Tu publicación \"" + alojamiento.getTitulo() + "\" ahora está "
                        + nuevoEstado + ".");
        return dto;
    }

    private AlojamientoDTO cambiarEstado(Alojamiento alojamiento, String nuevoEstado, Long usuarioId, String accion,
                                         String motivo) {
        String anterior = alojamiento.getEstado();
        MaquinaEstados.validarPublicacion(anterior, nuevoEstado);
        alojamiento.setEstado(nuevoEstado);
        alojamientoRepository.save(alojamiento);
        eventoAuditoriaServiceAPI.registrar(usuarioId, "ALOJAMIENTO", alojamiento.getId(), accion,
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("estadoAnterior", anterior, "estadoNuevo", nuevoEstado,
                        "motivo", motivo));
        return aDTO(alojamiento);
    }

    /** HU-42: las reservas futuras del alojamiento bloqueado pasan a revisión manual del administrador. */
    private void marcarReservasParaRevision(Long adminId, Alojamiento alojamiento) {
        List<Reserva> futuras = reservaRepository.buscarFuturas(alojamiento.getId(),
                List.of(Constantes.RESERVA_CONFIRMADA, Constantes.RESERVA_PENDIENTE_PAGO, Constantes.RESERVA_EN_REVISION),
                LocalDate.now());
        for (Reserva reserva : futuras) {
            reserva.setRequiereRevision(true);
            reservaRepository.save(reserva);
            eventoAuditoriaServiceAPI.registrar(adminId, "RESERVA", reserva.getId(), "MARCAR_REVISION_MANUAL",
                    Constantes.AUDITORIA_EXITO, Utilidad.detalle("alojamientoId", alojamiento.getId()));
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_PUBLICACION_ESTADO, reserva.getHuespedId(),
                    "Tu reserva está en revisión", "El alojamiento \"" + alojamiento.getTitulo()
                            + "\" fue bloqueado por un administrador. Tu reserva se conserva y será revisada.");
        }
    }

    /** HU-11: información mínima antes de enviar a revisión. */
    private void validarInformacionMinima(Alojamiento alojamiento) {
        List<String> faltantes = new ArrayList<>();
        if (alojamiento.getDescripcion() == null || alojamiento.getDescripcion().length() < 20) {
            faltantes.add("descripción de al menos 20 caracteres");
        }
        if (alojamiento.getDireccion() == null || alojamiento.getDireccion().isBlank()) {
            faltantes.add("dirección");
        }
        if (alojamiento.getPoliticaId() == null) {
            faltantes.add("política de cancelación");
        }
        if (imagenRepository.countByAlojamientoId(alojamiento.getId()) == 0) {
            faltantes.add("al menos una imagen");
        }
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Falta información para enviar a revisión: " + String.join(", ", faltantes) + ".");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AlojamientoResumenDTO> pendientesDeRevision(Integer pagina, Integer tamano) {
        return PaginaResponse.desde(alojamientoRepository.findByEstadoOrderByCreadoEnAsc(
                Constantes.ALOJAMIENTO_PENDIENTE_REVISION, pagina(pagina, tamano)), this::aResumen);
    }

    @Override
    @Transactional
    public Imagen agregarImagen(Long anfitrionId, Long alojamientoId, ImagenRequest request) {
        obtenerPropio(anfitrionId, alojamientoId);
        validarCupoImagenes(alojamientoId);
        return guardarImagen(alojamientoId, request.getUrl().trim(), request.getOrden());
    }

    @Override
    @Transactional
    public Imagen subirImagen(Long anfitrionId, Long alojamientoId, byte[] datos, String tipoContenido, Integer orden) {
        obtenerPropio(anfitrionId, alojamientoId);
        validarCupoImagenes(alojamientoId);
        String url = almacenamientoImagenAdapter.guardar(datos, tipoContenido);
        Imagen imagen = guardarImagen(alojamientoId, url, orden);
        eventoAuditoriaServiceAPI.registrar(anfitrionId, "IMAGEN", imagen.getId(), "SUBIR_IMAGEN",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("alojamientoId", alojamientoId));
        return imagen;
    }

    private void validarCupoImagenes(Long alojamientoId) {
        if (imagenRepository.countByAlojamientoId(alojamientoId) >= 20) {
            throw new ReglaNegocioException("Una publicación admite máximo 20 imágenes.");
        }
    }

    private Imagen guardarImagen(Long alojamientoId, String url, Integer orden) {
        Imagen imagen = new Imagen();
        imagen.setAlojamientoId(alojamientoId);
        imagen.setUrl(url);
        imagen.setOrden(orden == null ? (int) imagenRepository.countByAlojamientoId(alojamientoId) : orden);
        return imagenRepository.save(imagen);
    }

    @Override
    @Transactional
    public void eliminarImagen(Long anfitrionId, Long alojamientoId, Long imagenId) {
        obtenerPropio(anfitrionId, alojamientoId);
        Imagen imagen = imagenRepository.findById(imagenId)
                .filter(i -> i.getAlojamientoId().equals(alojamientoId))
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada en esta publicación."));
        imagenRepository.delete(imagen);
        almacenamientoImagenAdapter.eliminar(imagen.getUrl());
    }

    /** HU-13: servicios adicionales con costo. */
    @Override
    @Transactional
    public ServicioAdicional agregarServicioAdicional(Long anfitrionId, Long alojamientoId,
                                                      ServicioAdicionalRequest request) {
        obtenerPropio(anfitrionId, alojamientoId);
        ServicioAdicional servicio = new ServicioAdicional();
        servicio.setAlojamientoId(alojamientoId);
        servicio.setNombre(request.getNombre().trim());
        servicio.setPrecio(request.getPrecio());
        return servicioAdicionalRepository.save(servicio);
    }

    @Override
    @Transactional
    public void eliminarServicioAdicional(Long anfitrionId, Long alojamientoId, Long servicioAdicionalId) {
        obtenerPropio(anfitrionId, alojamientoId);
        ServicioAdicional servicio = servicioAdicionalRepository.findById(servicioAdicionalId)
                .filter(s -> s.getAlojamientoId().equals(alojamientoId))
                .orElseThrow(() -> new ResourceNotFoundException("Servicio adicional no encontrado en esta publicación."));
        servicioAdicionalRepository.delete(servicio);
    }

    @Override
    public AlojamientoResumenDTO aResumen(Alojamiento a) {
        AlojamientoResumenDTO dto = new AlojamientoResumenDTO();
        dto.setId(a.getId());
        dto.setTitulo(a.getTitulo());
        dto.setCiudad(a.getCiudad());
        dto.setTipo(a.getTipo());
        dto.setCapacidad(a.getCapacidad());
        dto.setPrecioNoche(a.getPrecioNoche());
        dto.setModoReserva(a.getModoReserva());
        dto.setEstado(a.getEstado());
        List<Imagen> imagenes = imagenRepository.findByAlojamientoIdOrderByOrdenAsc(a.getId());
        dto.setImagenPrincipal(imagenes.isEmpty() ? null : imagenes.get(0).getUrl());
        dto.setPromedioCalificacion(redondear(resenaRepository.promedioPorAlojamiento(a.getId())));
        dto.setTotalResenas(resenaRepository.countByAlojamientoIdAndVisibleTrue(a.getId()));
        return dto;
    }

    @Override
    public AlojamientoDTO aDTO(Alojamiento a) {
        AlojamientoDTO dto = new AlojamientoDTO();
        dto.setId(a.getId());
        dto.setAnfitrionId(a.getAnfitrionId());
        dto.setAnfitrionNombre(usuarioRepository.findById(a.getAnfitrionId()).map(u -> u.getNombre()).orElse(null));
        dto.setTitulo(a.getTitulo());
        dto.setDescripcion(a.getDescripcion());
        dto.setReglas(a.getReglas());
        dto.setCiudad(a.getCiudad());
        dto.setDireccion(a.getDireccion());
        dto.setTipo(a.getTipo());
        dto.setCapacidad(a.getCapacidad());
        dto.setPrecioNoche(a.getPrecioNoche());
        dto.setTarifaLimpieza(a.getTarifaLimpieza());
        dto.setModoReserva(a.getModoReserva());
        dto.setEstado(a.getEstado());
        dto.setCreadoEn(a.getCreadoEn());
        dto.setPolitica(a.getPoliticaId() == null ? null : politicaCancelacionRepository.findById(a.getPoliticaId()).orElse(null));
        dto.setImagenes(imagenRepository.findByAlojamientoIdOrderByOrdenAsc(a.getId()));
        dto.setServiciosAdicionales(servicioAdicionalRepository.findByAlojamientoId(a.getId()));
        List<Servicio> servicios = new ArrayList<>();
        for (AlojamientoServicio rel : alojamientoServicioRepository.findByAlojamientoId(a.getId())) {
            servicioRepository.findById(rel.getServicioId()).ifPresent(servicios::add);
        }
        dto.setServicios(servicios);
        dto.setPromedioCalificacion(redondear(resenaRepository.promedioPorAlojamiento(a.getId())));
        dto.setTotalResenas(resenaRepository.countByAlojamientoIdAndVisibleTrue(a.getId()));
        return dto;
    }

    private Double redondear(Double valor) {
        return valor == null ? null : Math.round(valor * 10.0) / 10.0;
    }

    private PageRequest pagina(Integer pagina, Integer tamano) {
        return PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
    }

    @Override
    public byte[] leerImagen(String nombre) {
        return almacenamientoImagenAdapter.leer(nombre);
    }

    @Override
    public String tipoImagen(String nombre) {
        return almacenamientoImagenAdapter.tipoContenido(nombre);
    }
}
