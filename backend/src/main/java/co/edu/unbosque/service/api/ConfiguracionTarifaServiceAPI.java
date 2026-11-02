package co.edu.unbosque.service.api;

import java.math.BigDecimal;
import co.edu.unbosque.entity.ConfiguracionTarifa;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * Parámetros globales de tarifas (HU-24).
 */
public interface ConfiguracionTarifaServiceAPI extends GenericServiceAPI<ConfiguracionTarifa, Long> {

    BigDecimal valor(String clave, BigDecimal porDefecto);

    ConfiguracionTarifa actualizar(Long adminId, String clave, BigDecimal valor);
}
