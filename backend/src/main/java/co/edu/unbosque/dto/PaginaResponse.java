package co.edu.unbosque.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Respuesta paginada (ADR-08). Evita devolver listas completas en consultas grandes.
 */
public class PaginaResponse<T> {

    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;

    public PaginaResponse() {
    }

    public static <E, T> PaginaResponse<T> desde(Page<E> page, Function<E, T> conversor) {
        PaginaResponse<T> r = new PaginaResponse<>();
        r.contenido = page.getContent().stream().map(conversor).toList();
        r.pagina = page.getNumber();
        r.tamano = page.getSize();
        r.totalElementos = page.getTotalElements();
        r.totalPaginas = page.getTotalPages();
        return r;
    }

    public static <T> PaginaResponse<T> desde(Page<T> page) {
        return desde(page, Function.identity());
    }

    public List<T> getContenido() { return contenido; }
    public void setContenido(List<T> contenido) { this.contenido = contenido; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }

    public long getTotalElementos() { return totalElementos; }
    public void setTotalElementos(long totalElementos) { this.totalElementos = totalElementos; }

    public int getTotalPaginas() { return totalPaginas; }
    public void setTotalPaginas(int totalPaginas) { this.totalPaginas = totalPaginas; }
}
