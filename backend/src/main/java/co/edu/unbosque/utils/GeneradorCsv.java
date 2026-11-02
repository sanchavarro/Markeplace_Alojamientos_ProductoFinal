package co.edu.unbosque.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Genera archivos CSV que Excel abre directamente (separador ; y BOM UTF-8 para las tildes).
 */
public final class GeneradorCsv {

    private GeneradorCsv() {
    }

    public static byte[] generar(List<String> encabezados, List<List<String>> filas) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(linea(encabezados));
        for (List<String> fila : filas) {
            sb.append(linea(fila));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String linea(List<String> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            String v = valores.get(i) == null ? "" : valores.get(i);
            if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            }
            sb.append(v);
        }
        return sb.append("\r\n").toString();
    }
}
