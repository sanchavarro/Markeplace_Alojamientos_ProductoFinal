package co.edu.unbosque.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Genera un archivo Excel (.xlsx) sin librerías externas (HU-47).
 * Un .xlsx es un ZIP con unos pocos XML (formato Office Open XML); aquí se arma la hoja más simple:
 * una fila de encabezados en negrita y las filas de datos como texto o número.
 */
public final class GeneradorExcel {

    private GeneradorExcel() {
    }

    public static byte[] generar(String nombreHoja, List<String> encabezados, List<List<String>> filas) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(salida)) {
            agregar(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                    </Types>""");
            agregar(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            agregar(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets><sheet name="%s" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>""".formatted(escapar(nombreHoja)));
            agregar(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                    </Relationships>""");
            agregar(zip, "xl/styles.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
                    <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
                    <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                    <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                    <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
                    <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                    </styleSheet>""");
            agregar(zip, "xl/worksheets/sheet1.xml", hoja(encabezados, filas));
            zip.finish();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
    }

    private static String hoja(List<String> encabezados, List<List<String>> filas) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
           .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        fila(xml, 1, encabezados, true);
        int numero = 2;
        for (List<String> fila : filas) {
            fila(xml, numero++, fila, false);
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private static void fila(StringBuilder xml, int numero, List<String> celdas, boolean negrita) {
        xml.append("<row r=\"").append(numero).append("\">");
        for (int i = 0; i < celdas.size(); i++) {
            String ref = columna(i) + numero;
            String valor = celdas.get(i) == null ? "" : celdas.get(i);
            if (!negrita && valor.matches("-?\\d+(\\.\\d+)?")) {
                xml.append("<c r=\"").append(ref).append("\"><v>").append(valor).append("</v></c>");
            } else {
                xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(negrita ? " s=\"1\"" : "")
                   .append("><is><t>").append(escapar(valor)).append("</t></is></c>");
            }
        }
        xml.append("</row>");
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    private static String columna(int indice) {
        StringBuilder letras = new StringBuilder();
        int n = indice + 1;
        while (n > 0) {
            int resto = (n - 1) % 26;
            letras.insert(0, (char) ('A' + resto));
            n = (n - 1) / 26;
        }
        return letras.toString();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void agregar(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
