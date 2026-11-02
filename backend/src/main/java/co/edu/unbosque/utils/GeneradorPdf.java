package co.edu.unbosque.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador de PDF muy simple (texto en Helvetica, varias páginas), sin librerías externas.
 * Suficiente para el reporte de reservas de la HU-47.
 */
public final class GeneradorPdf {

    private static final int LINEAS_POR_PAGINA = 48;

    private GeneradorPdf() {
    }

    public static byte[] generar(String titulo, List<String> lineas) {
        List<List<String>> paginas = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        for (String linea : lineas) {
            if (actual.size() == LINEAS_POR_PAGINA) {
                paginas.add(actual);
                actual = new ArrayList<>();
            }
            actual.add(linea);
        }
        paginas.add(actual);

        List<String> objetos = new ArrayList<>();
        int totalPaginas = paginas.size();
        // 1: catálogo, 2: páginas, 3: fuente, luego (página, contenido) por cada página
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < totalPaginas; i++) {
            kids.append(4 + i * 2).append(" 0 R ");
        }
        objetos.add("<< /Type /Pages /Kids [" + kids + "] /Count " + totalPaginas + " >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < totalPaginas; i++) {
            int contenido = 5 + i * 2;
            objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> "
                    + "/Contents " + contenido + " 0 R >>");
            String flujo = contenidoPagina(titulo, paginas.get(i), i + 1, totalPaginas);
            byte[] bytes = flujo.getBytes(StandardCharsets.ISO_8859_1);
            objetos.add("<< /Length " + bytes.length + " >>\nstream\n" + flujo + "\nendstream");
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribir(salida, "%PDF-1.4\n");
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            posiciones.add(salida.size());
            escribir(salida, (i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n");
        }
        int inicioXref = salida.size();
        StringBuilder xref = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (Integer pos : posiciones) {
            xref.append(String.format("%010d 00000 n \n", pos));
        }
        xref.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(inicioXref).append("\n%%EOF\n");
        escribir(salida, xref.toString());
        return salida.toByteArray();
    }

    private static String contenidoPagina(String titulo, List<String> lineas, int pagina, int total) {
        StringBuilder sb = new StringBuilder("BT\n/F1 14 Tf\n50 750 Td\n(" + escapar(titulo) + ") Tj\n");
        sb.append("/F1 9 Tf\n0 -14 Td\n(Pagina ").append(pagina).append(" de ").append(total).append(") Tj\n");
        sb.append("0 -20 Td\n");
        for (String linea : lineas) {
            sb.append("(").append(escapar(linea)).append(") Tj\n0 -14 Td\n");
        }
        return sb.append("ET").toString();
    }

    private static String escapar(String texto) {
        String t = texto == null ? "" : texto;
        return t.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static void escribir(ByteArrayOutputStream salida, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.ISO_8859_1);
        salida.write(bytes, 0, bytes.length);
    }
}
