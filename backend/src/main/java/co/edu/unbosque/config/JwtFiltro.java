package co.edu.unbosque.config;

import org.slf4j.MDC;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.CredencialesInvalidasException;
import co.edu.unbosque.utils.JwtUtil;
import co.edu.unbosque.utils.ListaTokensRevocados;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro JWT (ADR-05, tácticas T9 y T11).
 * En cada petición: valida firma y expiración, revisa que el token no esté revocado (logout)
 * y que el usuario siga ACTIVO y sin bloqueo. Si todo está bien, deja el id del usuario y su rol en el contexto.
 */
public class JwtFiltro extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO = "usuarioId";

    private final JwtUtil jwtUtil;
    private final ListaTokensRevocados listaTokensRevocados;
    private final UsuarioServiceAPI usuarioServiceAPI;

    public JwtFiltro(JwtUtil jwtUtil, ListaTokensRevocados listaTokensRevocados, UsuarioServiceAPI usuarioServiceAPI) {
        this.jwtUtil = jwtUtil;
        this.listaTokensRevocados = listaTokensRevocados;
        this.usuarioServiceAPI = usuarioServiceAPI;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.validar(encabezado.substring(7), JwtUtil.TIPO_ACCESO);
                if (listaTokensRevocados.estaRevocado(claims.getId())) {
                    throw new CredencialesInvalidasException("La sesión fue cerrada. Inicia sesión de nuevo.");
                }
                Long usuarioId = Long.valueOf(claims.getSubject());
                Usuario usuario = usuarioServiceAPI.get(usuarioId);
                if (usuario == null || !Constantes.USUARIO_ACTIVO.equals(usuario.getEstado())) {
                    throw new CredencialesInvalidasException("La cuenta no está activa.");
                }
                UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                        usuarioId, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol())));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
                request.setAttribute(ATRIBUTO_USUARIO, usuarioId);
                MDC.put(FiltroCorrelacion.MDC_USUARIO, String.valueOf(usuarioId));
            } catch (CredencialesInvalidasException e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"mensaje\":\"" + e.getMessage() + "\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
