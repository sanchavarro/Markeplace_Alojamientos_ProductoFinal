package co.edu.unbosque.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Adaptador de correo (infraestructura). Es la única clase que conoce el servidor SMTP.
 * correo.modo=simulado → no envía nada: deja el aviso en el log y guarda una vista previa HTML del correo
 *                        en correo.vista.previa.ruta (logs/correos) para revisar el diseño.
 * correo.modo=smtp     → envía el correo real (HTML + texto plano) con los datos de spring.mail.*.
 */
@Component
public class EnviadorCorreo {

    private static final Logger logger = Logger.getLogger(EnviadorCorreo.class.getName());
    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public static final String MODO_SIMULADO = "simulado";

    @Autowired
    private JavaMailSender mailSender;

    @Value("${correo.modo}")
    private String modo;

    @Value("${correo.remitente}")
    private String remitente;

    @Value("${correo.nombre.remitente:Marketplace de Alojamientos}")
    private String nombreRemitente;

    @Value("${correo.vista.previa.ruta:logs/correos}")
    private String rutaVistaPrevia;

    public boolean esSimulado() {
        return !"smtp".equalsIgnoreCase(modo);
    }

    /** Envía un correo con versión HTML y versión de texto plano. */
    public void enviar(String destino, PlantillaCorreo.Correo correo) {
        if (esSimulado()) {
            guardarVistaPrevia(correo);
            logger.info("canal=correo_simulado destino=" + Utilidad.ocultarCorreo(destino) + " asunto=\"" + correo.asunto() + "\"");
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            ayudante.setFrom(remitente, nombreRemitente);
            ayudante.setTo(destino);
            ayudante.setSubject(correo.asunto());
            ayudante.setText(correo.texto(), correo.html());
            mailSender.send(mensaje);
        } catch (MessagingException | IOException e) {
            // Sube al DespachadorOutbox para que reintente (ADR-07).
            throw new MailSendException("No se pudo armar el correo: " + e.getMessage(), e);
        }
        logger.info("canal=correo destino=" + Utilidad.ocultarCorreo(destino) + " asunto=\"" + correo.asunto() + "\" estado=enviado");
    }

    /** Guarda el HTML en disco para ver cómo se vería el correo sin un servidor SMTP. */
    private void guardarVistaPrevia(PlantillaCorreo.Correo correo) {
        if (rutaVistaPrevia == null || rutaVistaPrevia.isBlank()) {
            return;
        }
        try {
            Path carpeta = Paths.get(rutaVistaPrevia);
            Files.createDirectories(carpeta);
            // El nombre del archivo no usa datos del usuario (evita rutas construidas con datos externos).
            Path archivo = carpeta.resolve(LocalDateTime.now().format(MARCA) + "_" + UUID.randomUUID() + ".html");
            Files.writeString(archivo, correo.html(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.log(Level.WARNING, "No se pudo guardar la vista previa del correo", e);
        }
    }
}
