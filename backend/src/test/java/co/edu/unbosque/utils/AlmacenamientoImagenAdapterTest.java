package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** HU-07: el adaptador guarda solo imágenes válidas y no deja leer archivos fuera de su carpeta. */
class AlmacenamientoImagenAdapterTest {

    @Test
    void guardaLeeYRechazaArchivosInvalidos() throws Exception {
        Path carpeta = Files.createTempDirectory("imagenes");
        AlmacenamientoImagenAdapter adaptador = new AlmacenamientoImagenAdapter(carpeta.toString(),
                "http://localhost:8080/imagenes");
        byte[] datos = new byte[] { 1, 2, 3 };

        String url = adaptador.guardar(datos, "image/png");
        assertTrue(url.startsWith("http://localhost:8080/imagenes/") && url.endsWith(".png"));
        String nombre = url.substring(url.lastIndexOf('/') + 1);
        assertArrayEquals(datos, adaptador.leer(nombre));

        assertThrows(ReglaNegocioException.class, () -> adaptador.guardar(datos, "application/pdf"));
        assertThrows(ResourceNotFoundException.class, () -> adaptador.leer("../application.properties"));
    }
}
