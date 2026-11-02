package co.edu.unbosque.service.impl;

import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.repository.ReservaRepository;
import java.util.List;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.ActualizarPerfilRequest;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.UsuarioDTO;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.CredencialesInvalidasException;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class UsuarioServiceImpl extends GenericServiceImpl<Usuario, Long> implements UsuarioServiceAPI {

    /** Reservas que impiden desactivar la cuenta (HU-06). */
    private static final List<String> ESTADOS_ACTIVOS = List.of(Constantes.RESERVA_CONFIRMADA,
            Constantes.RESERVA_PENDIENTE_PAGO, Constantes.RESERVA_EN_REVISION);

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    @Override
    public CrudRepository<Usuario, Long> getDao() {
        return usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO perfil(Long usuarioId) {
        return aDTO(obtener(usuarioId));
    }

    /** HU-05: actualiza nombre, teléfono y foto y, si se pide, la clave (verificando la actual). El correo no cambia aquí. */
    @Override
    @Transactional
    public UsuarioDTO actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = obtener(usuarioId);
        usuario.setNombre(request.getNombre().trim());
        usuario.setTelefono(vacioANulo(request.getTelefono()));
        usuario.setFotoUrl(vacioANulo(request.getFotoUrl()));
        boolean cambiaClave = request.getNuevaClave() != null && !request.getNuevaClave().isBlank();
        if (cambiaClave) {
            if (request.getClaveActual() == null
                    || !passwordEncoder.matches(request.getClaveActual(), usuario.getHashContrasena())) {
                throw new CredencialesInvalidasException("La clave actual no es correcta.");
            }
            usuario.setHashContrasena(passwordEncoder.encode(request.getNuevaClave()));
        }
        usuarioRepository.save(usuario);
        eventoAuditoriaServiceAPI.registrar(usuarioId, "USUARIO", usuarioId, "ACTUALIZAR_PERFIL",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("cambioClave", cambiaClave));
        return aDTO(usuario);
    }

    /** HU-06: el usuario desactiva su cuenta. Para volver a activarla usa POST /auth/reactivar. */
    @Override
    @Transactional
    public UsuarioDTO desactivarCuenta(Long usuarioId) {
        Usuario usuario = obtener(usuarioId);
        if (Constantes.ROL_ADMINISTRADOR.equals(usuario.getRol())) {
            throw new ReglaNegocioException("La cuenta del administrador no se puede desactivar.");
        }
        LocalDate hoy = LocalDate.now();
        if (reservaRepository.contarActivasComoHuesped(usuarioId, ESTADOS_ACTIVOS, hoy) > 0
                || reservaRepository.contarActivasComoAnfitrion(usuarioId, ESTADOS_ACTIVOS, hoy) > 0) {
            throw new ConflictoException("No puedes desactivar la cuenta mientras tengas reservas confirmadas pendientes.");
        }
        usuario.setEstado(Constantes.USUARIO_INACTIVO);
        usuarioRepository.save(usuario);
        eventoAuditoriaServiceAPI.registrar(usuarioId, "USUARIO", usuarioId, "DESACTIVAR_CUENTA",
                Constantes.AUDITORIA_EXITO, null);
        return aDTO(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioDTO> listar(String estado, Integer pagina, Integer tamano) {
        PageRequest page = PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo), Sort.by("id"));
        Page<Usuario> resultado = (estado == null || estado.isBlank())
                ? usuarioRepository.findAll(page)
                : usuarioRepository.findByEstado(estado.toUpperCase(), page);
        return PaginaResponse.desde(resultado, this::aDTO);
    }

    /** HU-44: el administrador restringe (RESTRINGIDO) o reactiva (ACTIVO) a un usuario. */
    @Override
    @Transactional
    public UsuarioDTO cambiarEstadoPorAdministrador(Long adminId, Long usuarioId, String nuevoEstado, String motivo) {
        Usuario usuario = obtener(usuarioId);
        if (Constantes.ROL_ADMINISTRADOR.equals(usuario.getRol())) {
            throw new ReglaNegocioException("No se puede cambiar el estado de un administrador.");
        }
        String anterior = usuario.getEstado();
        usuario.setEstado(nuevoEstado);
        if (Constantes.USUARIO_ACTIVO.equals(nuevoEstado)) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }
        usuarioRepository.save(usuario);
        eventoAuditoriaServiceAPI.registrar(adminId, "USUARIO", usuarioId,
                Constantes.USUARIO_RESTRINGIDO.equals(nuevoEstado) ? "RESTRINGIR_USUARIO" : "REACTIVAR_USUARIO",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("estadoAnterior", anterior, "estadoNuevo", nuevoEstado,
                        "motivo", motivo));
        eventoOutboxServiceAPI.publicar(Constantes.EVENTO_USUARIO_ESTADO, usuarioId, "Estado de tu cuenta",
                Constantes.USUARIO_RESTRINGIDO.equals(nuevoEstado)
                        ? "Tu cuenta fue restringida por un administrador."
                        : "Tu cuenta fue reactivada.");
        return aDTO(usuario);
    }

    @Override
    public UsuarioDTO aDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setRol(usuario.getRol());
        dto.setEstado(usuario.getEstado());
        dto.setTelefono(usuario.getTelefono());
        dto.setFotoUrl(usuario.getFotoUrl());
        dto.setCreadoEn(usuario.getCreadoEn());
        return dto;
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
