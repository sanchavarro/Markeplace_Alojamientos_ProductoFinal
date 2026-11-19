package co.edu.unbosque.utils;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.service.api.UsuarioServiceAPI;

/**
 * Crea el canal "correo": busca al usuario, arma el correo con PlantillaCorreo (HTML + texto plano)
 * y lo envía con EnviadorCorreo.
 * Si el envío falla, la excepción sube al DespachadorOutbox, que reintenta hasta 3 veces (ADR-07).
 */
@Component
public class NotificadorCorreo extends Notificador {

    @Autowired
    private UsuarioServiceAPI usuarioServiceAPI;

    @Autowired
    private EnviadorCorreo enviadorCorreo;

    @Value("${app.frontend.url}")
    private String urlFrontend;

    private static final Logger logger = Logger.getLogger(NotificadorCorreo.class.getName());

    /** Avisa al arrancar si los enlaces de los correos van a apuntar a localhost. */
    @PostConstruct
    void revisarUrlFrontend() {
        if (apuntaALocalhost(urlFrontend)) {
            logger.warning("app.frontend.url=" + urlFrontend + " : los enlaces de los correos solo abrirán en este "
                    + "computador. En el despliegue configura FRONTEND_URL con la URL pública del frontend.");
        }
    }

    static boolean apuntaALocalhost(String url) {
        return url == null || url.contains("localhost") || url.contains("127.0.0.1");
    }

    @Override
    protected CanalNotificacion crearCanal() {
        return (tipoEvento, carga) -> {
            Usuario usuario = usuarioServiceAPI.obtener(carga.getUsuarioId());
            PlantillaCorreo.Correo correo = PlantillaCorreo.construir(tipoEvento, usuario.getNombre(), usuario.getCorreo(),
                    usuario.getRol(), carga.getTitulo(), carga.getMensaje(), carga.getEnlace(), urlFrontend);
            enviadorCorreo.enviar(usuario.getCorreo(), correo);
        };
    }
}
