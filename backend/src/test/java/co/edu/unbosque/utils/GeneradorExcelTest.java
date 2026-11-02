package co.edu.unbosque.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** HU-47: el .xlsx generado a mano tiene las partes que Excel necesita y escapa los caracteres especiales. */
class GeneradorExcelTest {

    @Test
    void generaUnLibroValido() throws Exception {
        byte[] archivo = GeneradorExcel.generar("Reservas", List.of("id", "ciudad"),
                List.of(List.of("1", "Bogotá & <Chía>")));

        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archivo))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                partes.put(entrada.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertTrue(partes.containsKey("[Content_Types].xml"));
        assertTrue(partes.containsKey("xl/workbook.xml"));
        assertTrue(partes.get("xl/worksheets/sheet1.xml").contains("Bogotá &amp; &lt;Chía&gt;"));
        assertTrue(partes.get("xl/worksheets/sheet1.xml").contains("<v>1</v>"));
    }
}
