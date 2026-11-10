package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import co.edu.unbosque.dto.CargaNotificacion;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/** Envío de correos (HU-36): SMTP real con HTML + texto, vista previa en modo simulado y canal del outbox. */
class CorreoTest {

    private static final PlantillaCorreo.Correo CORREO = new PlantillaCorreo.Correo("Asunto", "Texto", "<p>HTML</p>");

    private EnviadorCorreo enviador(String modo, JavaMailSender sender, String ruta) {
        EnviadorCorreo e = new EnviadorCorreo();
        ReflectionTestUtils.setField(e, "mailSender", sender);
        ReflectionTestUtils.setField(e, "modo", modo);
        ReflectionTestUtils.setField(e, "remitente", "no-reply@marketplace.com");
        ReflectionTestUtils.setField(e, "nombreRemitente", "Marketplace");
        ReflectionTestUtils.setField(e, "rutaVistaPrevia", ruta);
        return e;
    }

    @Test
    void enModoSmtpEnviaUnMensajeMimeConHtml() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mensaje = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(mensaje);
        EnviadorCorreo e = enviador("smtp", sender, "");

        e.enviar("ana@prueba.com", CORREO);

        verify(sender).send(mensaje);
        assertEquals("Asunto", mensaje.getSubject());
        assertTrue(!e.esSimulado());
    }

    @Test
    void enModoSimuladoGuardaLaVistaPreviaYNoEnvia(@TempDir Path carpeta) throws IOException {
        JavaMailSender sender = mock(JavaMailSender.class);
        EnviadorCorreo e = enviador("simulado", sender, carpeta.toString());

        e.enviar("ana@prueba.com", CORREO);

        verify(sender, never()).send(any(MimeMessage.class));
        assertTrue(e.esSimulado());
        try (Stream<Path> archivos = Files.list(carpeta)) {
            Path html = archivos.findFirst().orElseThrow();
            assertTrue(html.toString().endsWith(".html"));
            assertEquals("<p>HTML</p>", Files.readString(html));
        }
    }

    @Test
    void sinRutaDeVistaPreviaNoEscribeNada() {
        JavaMailSender sender = mock(JavaMailSender.class);
        enviador("simulado", sender, "").enviar("ana@prueba.com", CORREO);
        verify(sender, never()).createMimeMessage();
    }

    @Test
    void elCanalDeCorreoUsaLaPlantillaConLosDatosDelUsuario() {
        UsuarioServiceAPI usuarios = mock(UsuarioServiceAPI.class);
        EnviadorCorreo enviador = mock(EnviadorCorreo.class);
        Usuario u = new Usuario();
        u.setNombre("Ana");
        u.setCorreo("ana@prueba.com");
        u.setRol(Constantes.ROL_HUESPED);
        when(usuarios.obtener(7L)).thenReturn(u);

        NotificadorCorreo notificador = new NotificadorCorreo();
        ReflectionTestUtils.setField(notificador, "usuarioServiceAPI", usuarios);
        ReflectionTestUtils.setField(notificador, "enviadorCorreo", enviador);
        ReflectionTestUtils.setField(notificador, "urlFrontend", "http://localhost:5173");

        CargaNotificacion carga = new CargaNotificacion();
        carga.setUsuarioId(7L);
        carga.setTitulo("Reserva confirmada");
        carga.setMensaje("Tu reserva quedó confirmada.");
        notificador.notificar(Constantes.EVENTO_RESERVA_CONFIRMADA, carga);

        verify(enviador).enviar(eq("ana@prueba.com"),
                argThat(c -> c.html().contains("/mis-reservas") && c.asunto().startsWith("Reserva confirmada")));
    }
}
