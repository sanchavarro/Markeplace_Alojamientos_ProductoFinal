package co.edu.unbosque.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.LoginRequest;
import co.edu.unbosque.dto.LoginResponse;
import co.edu.unbosque.dto.RegistroRequest;
import co.edu.unbosque.dto.RestablecerClaveRequest;
import co.edu.unbosque.dto.UsuarioDTO;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.service.api.AuthServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.CredencialesInvalidasException;
import co.edu.unbosque.utils.CuentaBloqueadaException;
import co.edu.unbosque.utils.EnviadorCorreo;
import co.edu.unbosque.utils.JwtUtil;
import co.edu.unbosque.utils.ListaTokensRevocados;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.Utilidad;
import io.jsonwebtoken.Claims;

/**
 * Registro, inicio y cierre de sesión, y recuperación de clave (HU-01, HU-02, HU-04, HU-06).
 * ADR-05: JWT de 60 min, BCrypt y bloqueo de 15 min después de 5 intentos fallidos.
 */
@Service
public class AuthServiceImpl implements AuthServiceAPI {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioServiceAPI usuarioServiceAPI;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EnviadorCorreo enviadorCorreo;

    @Value("${app.frontend.url}")
    private String urlFrontend;

    @Autowired
    private ListaTokensRevocados listaTokensRevocados;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    @Value("${seguridad.intentos.maximos}")
    private int intentosMaximos;

    @Value("${seguridad.bloqueo.minutos}")
    private int minutosBloqueo;

    /** HU-01: registro con rol HUESPED o ANFITRION. La clave se guarda con BCrypt. */
    @Override
    @Transactional
    public UsuarioDTO registrar(RegistroRequest request) {
        String rol = request.getRol().trim().toUpperCase();
        if (!Constantes.ROLES_REGISTRABLES.contains(rol)) {
            throw new ReglaNegocioException("El rol debe ser HUESPED o ANFITRION.");
        }
        String correo = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("El correo ya está registrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setCorreo(correo);
        usuario.setHashContrasena(passwordEncoder.encode(request.getClave()));
        usuario.setRol(rol);
        usuario.setEstado(Constantes.USUARIO_ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setCreadoEn(LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);

        eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "REGISTRO",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("rol", rol));
        eventoOutboxServiceAPI.publicar(Constantes.EVENTO_USUARIO_REGISTRADO, usuario.getId(),
                "Bienvenido al Marketplace", "Hola " + usuario.getNombre() + ", tu cuenta fue creada.");
        return usuarioServiceAPI.aDTO(usuario);
    }

    /**
     * HU-02: inicio de sesión. No se revierte al fallar, para que el contador de intentos
     * y la auditoría del intento fallido sí queden guardados.
     */
    @Override
    @Transactional(noRollbackFor = { CredencialesInvalidasException.class, CuentaBloqueadaException.class })
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = verificarCredenciales(request, false);
        return construirLogin(usuario);
    }

    /** HU-06: un usuario INACTIVO vuelve a activar su cuenta con sus credenciales. */
    @Override
    @Transactional(noRollbackFor = { CredencialesInvalidasException.class, CuentaBloqueadaException.class })
    public LoginResponse reactivar(LoginRequest request) {
        Usuario usuario = verificarCredenciales(request, true);
        if (Constantes.USUARIO_INACTIVO.equals(usuario.getEstado())) {
            usuario.setEstado(Constantes.USUARIO_ACTIVO);
            usuarioRepository.save(usuario);
            eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "REACTIVAR_CUENTA",
                    Constantes.AUDITORIA_EXITO, null);
        }
        return construirLogin(usuario);
    }

    private Usuario verificarCredenciales(LoginRequest request, boolean permitirInactivo) {
        Optional<Usuario> encontrado = usuarioRepository.findByCorreoIgnoreCase(request.getCorreo().trim());
        if (encontrado.isEmpty()) {
            eventoAuditoriaServiceAPI.registrar(null, "USUARIO", null, "LOGIN", Constantes.AUDITORIA_FALLO,
                    Utilidad.detalle("motivo", "correo no registrado"));
            throw new CredencialesInvalidasException("Correo o clave incorrectos.");
        }
        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now();

        if (Constantes.USUARIO_RESTRINGIDO.equals(usuario.getEstado())) {
            eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "LOGIN",
                    Constantes.AUDITORIA_FALLO, Utilidad.detalle("motivo", "cuenta restringida"));
            throw new CuentaBloqueadaException("Tu cuenta está restringida por un administrador.");
        }
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            throw new CuentaBloqueadaException("Cuenta bloqueada por intentos fallidos hasta las "
                    + usuario.getBloqueadoHasta().format(HORA) + ".");
        }
        if (!passwordEncoder.matches(request.getClave(), usuario.getHashContrasena())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            if (intentos >= intentosMaximos) {
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(ahora.plusMinutes(minutosBloqueo));
                usuarioRepository.save(usuario);
                eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(),
                        "BLOQUEO_POR_INTENTOS", Constantes.AUDITORIA_EXITO,
                        Utilidad.detalle("minutos", minutosBloqueo));
                throw new CuentaBloqueadaException("Superaste " + intentosMaximos
                        + " intentos fallidos. La cuenta queda bloqueada " + minutosBloqueo + " minutos.");
            }
            usuario.setIntentosFallidos(intentos);
            usuarioRepository.save(usuario);
            eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "LOGIN",
                    Constantes.AUDITORIA_FALLO, Utilidad.detalle("intento", intentos));
            throw new CredencialesInvalidasException("Correo o clave incorrectos. Te quedan "
                    + (intentosMaximos - intentos) + " intentos.");
        }
        if (Constantes.USUARIO_INACTIVO.equals(usuario.getEstado()) && !permitirInactivo) {
            throw new CuentaBloqueadaException("Tu cuenta está desactivada. Reactívala en POST /auth/reactivar.");
        }
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "LOGIN",
                Constantes.AUDITORIA_EXITO, null);
        return usuario;
    }

    private LoginResponse construirLogin(Usuario usuario) {
        LoginResponse respuesta = new LoginResponse();
        respuesta.setToken(jwtUtil.generarTokenAcceso(usuario.getId(), usuario.getRol()));
        respuesta.setTipo("Bearer");
        respuesta.setExpiraEnSegundos(jwtUtil.getExpiracionMs() / 1000);
        respuesta.setUsuario(usuarioServiceAPI.aDTO(usuario));
        return respuesta;
    }

    /** HU-02: cierre de sesión. El token queda revocado hasta que expire (T11 Revocar acceso). */
    @Override
    @Transactional
    public void logout(String token) {
        Claims claims = jwtUtil.validar(token, JwtUtil.TIPO_ACCESO);
        listaTokensRevocados.revocar(claims.getId(), claims.getExpiration());
        Long usuarioId = Long.valueOf(claims.getSubject());
        eventoAuditoriaServiceAPI.registrar(usuarioId, "USUARIO", usuarioId, "LOGOUT", Constantes.AUDITORIA_EXITO, null);
    }

    /**
     * HU-04 Recuperación de clave: genera un token de 30 min y deja en el outbox el correo con el enlace
     * a la página de restablecer del frontend. Solo en correo.modo=simulado el token también se devuelve
     * en la respuesta, para poder probar sin correo real (Postman y pruebas automáticas).
     */
    @Override
    @Transactional
    public Map<String, Object> solicitarRecuperacion(String correo) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Si el correo está registrado, recibirás las instrucciones para restablecer la clave.");
        usuarioRepository.findByCorreoIgnoreCase(correo.trim()).ifPresent(usuario -> {
            String token = jwtUtil.generarTokenRecuperacion(usuario.getId());
            String enlace = urlFrontend + "/restablecer?token=" + token;
            eventoOutboxServiceAPI.publicar(Constantes.EVENTO_RECUPERACION_CLAVE, usuario.getId(),
                    "Recuperación de clave",
                    "Se solicitó restablecer tu clave. Abre este enlace para crear una nueva (vence en 30 minutos):",
                    enlace);
            eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "SOLICITAR_RECUPERACION",
                    Constantes.AUDITORIA_EXITO, null);
            if (enviadorCorreo.esSimulado()) {
                respuesta.put("tokenRecuperacionSimulado", token);
            }
        });
        return respuesta;
    }

    @Override
    @Transactional
    public void restablecerClave(RestablecerClaveRequest request) {
        Claims claims = jwtUtil.validar(request.getToken(), JwtUtil.TIPO_RECUPERACION);
        if (listaTokensRevocados.estaRevocado(claims.getId())) {
            throw new CredencialesInvalidasException("Este enlace de recuperación ya fue usado.");
        }
        Usuario usuario = usuarioServiceAPI.obtener(Long.valueOf(claims.getSubject()));
        usuario.setHashContrasena(passwordEncoder.encode(request.getNuevaClave()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        listaTokensRevocados.revocar(claims.getId(), claims.getExpiration());
        eventoAuditoriaServiceAPI.registrar(usuario.getId(), "USUARIO", usuario.getId(), "RESTABLECER_CLAVE",
                Constantes.AUDITORIA_EXITO, null);
    }
}
