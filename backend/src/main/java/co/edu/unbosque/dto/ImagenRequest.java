package co.edu.unbosque.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Imagen de una publicación.
 */
public class ImagenRequest {

    @NotBlank(message = "La URL de la imagen es obligatoria.")
    @Size(max = 255, message = "La URL admite máximo 255 caracteres.")
    @Pattern(regexp = "^https?://.+", message = "La URL debe empezar por http:// o https://.")
    private String url;

    @Min(value = 0, message = "El orden no puede ser negativo.")
    private Integer orden;

    public ImagenRequest() {
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
