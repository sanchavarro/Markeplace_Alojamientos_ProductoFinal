package co.edu.unbosque.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.service.api.UsuarioServiceAPI;

/**
 * Crea el canal "correo": busca el correo del usuario y lo envía con EnviadorCorreo.
 * Si el envío falla, la excepción sube al DespachadorOutbox, que reintenta hasta 3 veces (ADR-07).
 */
@Component
public class NotificadorCorreo extends Notificador {

    @Autowired
    private UsuarioServiceAPI usuarioServiceAPI;

    @Autowired
    private EnviadorCorreo enviadorCorreo;

    @Override
    protected CanalNotificacion crearCanal() {
        return (tipoEvento, carga) -> {
            Usuario usuario = usuarioServiceAPI.obtener(carga.getUsuarioId());
            StringBuilder texto = new StringBuilder();
            texto.append("Hola ").append(usuario.getNombre()).append(",\n\n");
            texto.append(carga.getMensaje()).append("\n");
            if (carga.getEnlace() != null) {
                texto.append("\n").append(carga.getEnlace()).append("\n");
            }
            texto.append("\nMarketplace de Alojamientos - Inversiones LR");
            enviadorCorreo.enviar(usuario.getCorreo(), carga.getTitulo(), texto.toString());
        };
    }
}
