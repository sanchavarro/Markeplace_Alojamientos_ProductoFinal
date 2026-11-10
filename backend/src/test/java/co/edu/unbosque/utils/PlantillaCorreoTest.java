package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** Plantilla HTML de los correos (HU-36): contenido, escape de HTML y botón según el evento y el rol. */
class PlantillaCorreoTest {

    private static final String FRONT = "http://localhost:5173/";

    @Test
    void elCorreoDeVerificacionMuestraElCodigoYElBotonDeConfirmar() {
        PlantillaCorreo.Correo c = PlantillaCorreo.construir(Constantes.EVENTO_VERIFICACION_CORREO, "Ana", "ana@prueba.com",
                Constantes.ROL_HUESPED, "Confirma tu correo", "Tu código para confirmar la cuenta es 482913.", null, FRONT);
        assertTrue(c.html().contains("482913"));
        assertTrue(c.html().contains("http://localhost:5173/verificar-correo?correo=ana%40prueba.com"));
        assertTrue(c.texto().contains("Confirmar mi correo"));
        assertEquals("Confirma tu correo · Marketplace", c.asunto());
    }

    @Test
    void escapaElHtmlDeLosDatosDelUsuario() {
        PlantillaCorreo.Correo c = PlantillaCorreo.construir(Constantes.EVENTO_USUARIO_REGISTRADO, "<script>x</script>", "a@b.co",
                Constantes.ROL_HUESPED, "Bienvenido", "Hola <b>", null, FRONT);
        assertFalse(c.html().contains("<script>x</script>"));
        assertTrue(c.html().contains("&lt;script&gt;"));
    }

    @Test
    void elBotonDependeDelRolDelDestinatario() {
        PlantillaCorreo.Correo anfitrion = PlantillaCorreo.construir(Constantes.EVENTO_RESERVA_CONFIRMADA, "Luis", "l@b.co",
                Constantes.ROL_ANFITRION, "Nueva reserva", "Recibiste una reserva.", null, FRONT);
        PlantillaCorreo.Correo huesped = PlantillaCorreo.construir(Constantes.EVENTO_RESERVA_CONFIRMADA, "Ana", "a@b.co",
                Constantes.ROL_HUESPED, "Reserva confirmada", "Tu reserva quedó confirmada.", null, FRONT);
        assertTrue(anfitrion.html().contains("/anfitrion/reservas"));
        assertTrue(huesped.html().contains("/mis-reservas"));
    }

    @Test
    void laRecuperacionUsaElEnlaceDelEvento() {
        String enlace = "http://localhost:5173/restablecer?token=abc";
        PlantillaCorreo.Correo c = PlantillaCorreo.construir(Constantes.EVENTO_RECUPERACION_CLAVE, "Ana", "a@b.co",
                Constantes.ROL_HUESPED, "Recuperación de clave", "Abre el enlace.", enlace, FRONT);
        assertTrue(c.html().contains(enlace));
        assertTrue(c.texto().contains("Crear una nueva clave: " + enlace));
    }
}
