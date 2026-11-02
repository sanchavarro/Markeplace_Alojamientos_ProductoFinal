package co.edu.unbosque.utils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tokens cerrados con logout (HU-02, T11 Revocar acceso).
 * Guarda el identificador (jti) del token hasta que expira; el filtro JWT rechaza los que estén aquí.
 * Es un bean de Spring, por lo tanto una sola instancia compartida (singleton gestionado por Spring).
 */
@Component
public class ListaTokensRevocados {

    private final Map<String, Date> revocados = new ConcurrentHashMap<>();

    public void revocar(String jti, Date expiracion) {
        if (jti != null) {
            revocados.put(jti, expiracion == null ? new Date() : expiracion);
        }
    }

    public boolean estaRevocado(String jti) {
        return jti != null && revocados.containsKey(jti);
    }

    /** Limpia los tokens que ya expiraron, cada 10 minutos. */
    @Scheduled(fixedRate = 600000)
    public void limpiar() {
        Date ahora = new Date();
        revocados.entrySet().removeIf(e -> e.getValue().before(ahora));
    }
}
