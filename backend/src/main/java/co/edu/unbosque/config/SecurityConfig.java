package co.edu.unbosque.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.JwtUtil;
import co.edu.unbosque.utils.ListaTokensRevocados;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad (ADR-05, tácticas T9 Autenticar, T10 Autorizar, T13 Cifrar datos en tránsito).
 * API sin estado: cada petición trae su JWT. Las rutas se autorizan por rol.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String HUESPED = "HUESPED";
    private static final String ANFITRION = "ANFITRION";
    private static final String ADMIN = "ADMINISTRADOR";

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil, ListaTokensRevocados revocados,
                                           UsuarioServiceAPI usuarioServiceAPI) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        escribir(res, HttpServletResponse.SC_UNAUTHORIZED, "Debes iniciar sesión para usar este recurso."))
                .accessDeniedHandler((req, res, e) ->
                        escribir(res, HttpServletResponse.SC_FORBIDDEN, "Tu rol no tiene permiso para este recurso.")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Salud y métricas
                .requestMatchers(HttpMethod.GET, "/salud").permitAll()
                .requestMatchers(HttpMethod.GET, "/salud/metricas").hasRole(ADMIN)
                // Autenticación (HU-01, HU-02, HU-04, HU-06)
                .requestMatchers(HttpMethod.POST, "/auth/registro", "/auth/login", "/auth/reactivar",
                        "/auth/recuperar", "/auth/restablecer").permitAll()
                // Consulta pública: catálogo, búsqueda, cotización, detalle, calendario y reseñas
                .requestMatchers(HttpMethod.GET, "/catalogo/**", "/busqueda/**", "/tarifas").permitAll()
                .requestMatchers(HttpMethod.POST, "/cotizaciones").permitAll()
                .requestMatchers(HttpMethod.GET, "/alojamientos/mios").hasRole(ANFITRION)
                .requestMatchers(HttpMethod.GET, "/alojamientos/{id}", "/alojamientos/{id}/calendario",
                        "/alojamientos/{id}/resenas").permitAll()
                .requestMatchers(HttpMethod.GET, "/imagenes/**").permitAll()
                // HU-08: el dueño o el administrador editan (el servicio verifica que sea el dueño)
                .requestMatchers(HttpMethod.PUT, "/alojamientos/{id}").hasAnyRole(ANFITRION, ADMIN)
                // Publicaciones y disponibilidad: anfitrión (HU-07 a HU-18)
                .requestMatchers("/alojamientos/**").hasRole(ANFITRION)
                // Reservas (HU-26 a HU-35)
                .requestMatchers(HttpMethod.POST, "/reservas", "/reservas/{id}/pagar").hasRole(HUESPED)
                .requestMatchers(HttpMethod.GET, "/reservas/mias").hasRole(HUESPED)
                .requestMatchers("/reservas/anfitrion", "/reservas/{id}/aprobar", "/reservas/{id}/rechazar")
                        .hasRole(ANFITRION)
                .requestMatchers("/reservas/**").authenticated()
                // Reseñas (HU-38)
                .requestMatchers(HttpMethod.POST, "/resenas").hasRole(HUESPED)
                // Administración, reportes y tarifas (HU-24, HU-42 a HU-48)
                .requestMatchers("/admin/**", "/reportes/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.PUT, "/tarifas/**").hasRole(ADMIN)
                .anyRequest().authenticated())
            .addFilterBefore(new JwtFiltro(jwtUtil, revocados, usuarioServiceAPI),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS: solo el frontend puede llamar a la API desde el navegador. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(frontendUrl);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("X-Correlation-Id");
        config.addExposedHeader("Content-Disposition");
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void escribir(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
