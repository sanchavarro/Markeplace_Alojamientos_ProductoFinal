package co.edu.unbosque.service.api;

import java.util.Map;
import co.edu.unbosque.dto.SaludResponse;

/**
 * Observabilidad (T8 Monitor, HU-49): estado del sistema y métricas.
 */
public interface SaludServiceAPI {

    SaludResponse estado();

    Map<String, Object> metricas();
}
