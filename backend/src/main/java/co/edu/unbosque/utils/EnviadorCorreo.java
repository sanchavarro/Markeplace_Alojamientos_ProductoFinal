package co.edu.unbosque.utils;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de correo (infraestructura). Es la única clase que conoce el servidor SMTP.
 * correo.modo=simulado → solo escribe en el log (pruebas y CI).
 * correo.modo=smtp     → envía el correo real con los datos de spring.mail.*.
 */
@Component
public class EnviadorCorreo {

    private static final Logger logger = Logger.getLogger(EnviadorCorreo.class.getName());

    public static final String MODO_SIMULADO = "simulado";

    @Autowired
    private JavaMailSender mailSender;

    @Value("${correo.modo}")
    private String modo;

    @Value("${correo.remitente}")
    private String remitente;

    public boolean esSimulado() {
        return !"smtp".equalsIgnoreCase(modo);
    }

    public void enviar(String destino, String asunto, String texto) {
        if (esSimulado()) {
            logger.info("canal=correo_simulado destino=" + Utilidad.ocultarCorreo(destino) + " asunto=\"" + asunto + "\"");
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        mailSender.send(mensaje);
        logger.info("canal=correo destino=" + Utilidad.ocultarCorreo(destino) + " asunto=\"" + asunto + "\" estado=enviado");
    }
}
