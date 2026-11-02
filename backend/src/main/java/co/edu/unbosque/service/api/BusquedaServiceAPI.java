package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.AlojamientoDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import co.edu.unbosque.dto.AlojamientoResumenDTO;
import co.edu.unbosque.dto.PaginaResponse;

/**
 * IBusqueda del diagrama de componentes (épica E05, HU-19 a HU-22, ADR-08).
 */
public interface BusquedaServiceAPI {

    PaginaResponse<AlojamientoResumenDTO> buscar(String ciudad, String tipo, Integer capacidad,
                                                 BigDecimal precioMin, BigDecimal precioMax,
                                                 LocalDate llegada, LocalDate salida, List<Long> servicios,
                                                 Integer pagina, Integer tamano);

    /** HU-22: precio, capacidad, servicios, política y calificación de 2 o 3 alojamientos. */
    List<AlojamientoDTO> comparar(List<Long> ids);
}
