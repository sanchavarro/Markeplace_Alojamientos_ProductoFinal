package co.edu.unbosque.service.api;

import java.time.LocalDate;
import java.util.List;
import co.edu.unbosque.dto.CotizacionRequest;
import co.edu.unbosque.dto.CotizacionResponse;
import co.edu.unbosque.entity.Alojamiento;

/**
 * ICotizacion del diagrama de componentes (épica E06, HU-23 a HU-25).
 */
public interface CotizacionServiceAPI {

    /** Cotización pública: valida estado, capacidad, fechas y disponibilidad. */
    CotizacionResponse cotizar(CotizacionRequest request);

    /** Cálculo del precio (lo usa también la reserva). El total siempre se calcula en el servidor (T12). */
    CotizacionResponse calcular(Alojamiento alojamiento, LocalDate llegada, LocalDate salida, Integer numHuespedes,
                                List<Long> serviciosAdicionales);
}
