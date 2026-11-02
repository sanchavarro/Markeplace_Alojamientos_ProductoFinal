package co.edu.unbosque.utils;

import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los tokens JWT (ADR-05, táctica T9 Autenticar actores).
 * - Token de acceso: 60 minutos, lleva el id del usuario, su rol y un identificador único (jti).
 * - Token de recuperación de clave: 30 minutos, solo sirve para restablecer la clave (HU-04).
 */
@Component
public class JwtUtil {

    public static final String TIPO_ACCESO = "ACCESO";
    public static final String TIPO_RECUPERACION = "RECUPERACION";

    private final SecretKey llave;
    private final long expiracionMs;
    private final long expiracionRecuperacionMs;

    public JwtUtil(@Value("${jwt.secret}") String secreto,
                   @Value("${jwt.expiration}") long expiracionMs,
                   @Value("${jwt.recuperacion.expiration}") long expiracionRecuperacionMs) {
        this.llave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        this.expiracionMs = expiracionMs;
        this.expiracionRecuperacionMs = expiracionRecuperacionMs;
    }

    public String generarTokenAcceso(Long idUsuario, String rol) {
        return generar(idUsuario, rol, TIPO_ACCESO, expiracionMs);
    }

    public String generarTokenRecuperacion(Long idUsuario) {
        return generar(idUsuario, null, TIPO_RECUPERACION, expiracionRecuperacionMs);
    }

    private String generar(Long idUsuario, String rol, String tipo, long duracionMs) {
        Date ahora = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(idUsuario))
                .claim("rol", rol)
                .claim("tipo", tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs))
                .signWith(llave)
                .compact();
    }

    /** Valida firma y expiración. Lanza CredencialesInvalidasException si el token no sirve. */
    public Claims validar(String token, String tipoEsperado) {
        try {
            Claims claims = Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload();
            if (!tipoEsperado.equals(claims.get("tipo", String.class))) {
                throw new CredencialesInvalidasException("El token no es válido para esta operación.");
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new CredencialesInvalidasException("La sesión expiró. Inicia sesión de nuevo.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new CredencialesInvalidasException("Token inválido.");
        }
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}
