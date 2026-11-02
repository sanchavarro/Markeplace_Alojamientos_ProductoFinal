package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.ReporteExportacion;
import java.time.LocalDate;
import java.util.Map;

/**
 * IReportes del diagrama de componentes (épicas E12 y E13, HU-46 a HU-48).
 */
public interface ReporteServiceAPI {

    Map<String, Object> dashboard();

    /** Exporta de inmediato las reservas creadas entre dos fechas en csv, pdf o excel. Registra quién exporta (HU-48). */
    byte[] exportarReservas(Long adminId, String formato, LocalDate desde, LocalDate hasta);

    /** HU-47: pide el reporte; se genera en segundo plano y se avisa con una notificación cuando está listo. */
    ReporteExportacion solicitarExportacion(Long adminId, String formato, LocalDate desde, LocalDate hasta);

    PaginaResponse<ReporteExportacion> misExportaciones(Long adminId, Integer pagina, Integer tamano);

    ReporteExportacion obtenerExportacion(Long id);

    ReporteExportacion descargarExportacion(Long adminId, Long id);

    void procesarExportacion(Long id);

    int retomarExportacionesPendientes();

    String tipoContenido(String formato);

    String extension(String formato);
}
