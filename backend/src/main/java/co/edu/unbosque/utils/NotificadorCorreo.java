package co.edu.unbosque.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
