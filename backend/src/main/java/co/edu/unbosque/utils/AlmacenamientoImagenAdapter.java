package co.edu.unbosque.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PATRÓN ADAPTADOR (PAT-01) para el almacenamiento de imágenes (HU-07).
 * El negocio solo pide "guardar" y recibe una URL; no sabe dónde quedan los archivos.
 * Hoy se guardan en una carpeta local; para usar la nube (S3, Cloudinary) solo se cambia esta clase.
 */
@Component
public class AlmacenamientoImagenAdapter {

    private static final Logger logger = Logger.getLogger(AlmacenamientoImagenAdapter.class.getName());

    /** Tipos permitidos y su extensión. */
    private static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private final Path carpeta;
    private final String urlBase;

    public AlmacenamientoImagenAdapter(@Value("${almacenamiento.ruta}") String ruta,
                                       @Value("${almacenamiento.url.base}") String urlBase) {
        this.carpeta = Paths.get(ruta).toAbsolutePath().normalize();
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /** Guarda la imagen con un nombre aleatorio y devuelve su URL pública. */
    public String guardar(byte[] datos, String tipoContenido) {
        if (datos == null || datos.length == 0) {
            throw new ReglaNegocioException("El archivo de imagen está vacío.");
        }
        if (datos.length > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("La imagen supera el tamaño máximo de 5 MB.");
        }
        String extension = tipoContenido == null ? null : TIPOS.get(tipoContenido.toLowerCase());
        if (extension == null) {
            throw new ReglaNegocioException("Formato no permitido. Usa JPG, PNG o WEBP.");
        }
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(carpeta);
            Files.write(carpeta.resolve(nombre), datos);
        } catch (IOException e) {
            logger.severe("almacenamiento_fallo motivo=\"" + e.getMessage() + "\"");
            throw new ServicioNoDisponibleException("No se pudo guardar la imagen. Intenta de nuevo.");
        }
        return urlBase + "/" + nombre;
    }

    /** Lee una imagen guardada. Solo acepta nombres generados por guardar() (evita leer otros archivos). */
    public byte[] leer(String nombre) {
        if (!esNombreValido(nombre)) {
            throw new ResourceNotFoundException("Imagen no encontrada.");
        }
        try {
            return Files.readAllBytes(carpeta.resolve(nombre));
        } catch (IOException e) {
            throw new ResourceNotFoundException("Imagen no encontrada.");
        }
    }

    /** Borra el archivo si la URL apunta a este almacenamiento; si es una URL externa no hace nada. */
    public void eliminar(String url) {
        if (url == null || !url.startsWith(urlBase + "/")) {
            return;
        }
        String nombre = url.substring(urlBase.length() + 1);
        if (!esNombreValido(nombre)) {
            return;
        }
        try {
            Files.deleteIfExists(carpeta.resolve(nombre));
        } catch (IOException e) {
            logger.warning("almacenamiento_no_borrado nombre=" + nombre);
        }
    }

    public String tipoContenido(String nombre) {
        if (nombre.endsWith(".png")) {
            return "image/png";
        }
        if (nombre.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private boolean esNombreValido(String nombre) {
        return nombre != null && nombre.matches("^[0-9a-f\\-]{36}\\.(jpg|png|webp)$");
    }
}
