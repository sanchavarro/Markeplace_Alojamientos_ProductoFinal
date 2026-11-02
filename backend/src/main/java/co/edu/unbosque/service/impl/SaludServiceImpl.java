package co.edu.unbosque.service.impl;

import java.util.logging.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unbosque.dto.SaludResponse;
import co.edu.unbosque.service.api.EventoOutboxServiceAPI;
import co.edu.unbosque.service.api.SaludServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.MetricasSistema;
import co.edu.unbosque.utils.PasarelaPagoAdapter;

@Service
public class SaludServiceImpl implements SaludServiceAPI {

    @Autowired
    private PasarelaPagoAdapter pasarelaPagoAdapter;

    @Autowired
    private EventoOutboxServiceAPI eventoOutboxServiceAPI;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = Logger.getLogger(SaludServiceImpl.class.getName());

    @Override
    public SaludResponse estado() {
        SaludResponse respuesta = new SaludResponse();
        boolean baseDatosArriba = baseDatosResponde();
        boolean circuitoAbierto = "ABIERTO".equals(pasarelaPagoAdapter.getEstadoCortacircuitos());
        // HU-49: "caido" si no hay base de datos; "degradado" si la pasarela está fallando.
        respuesta.setEstado(!baseDatosArriba ? "caido" : circuitoAbierto ? "degradado" : "ok");
        respuesta.setBaseDatos(baseDatosArriba ? "UP" : "DOWN");
        respuesta.setAplicacion("Marketplace de Alojamientos - Inversiones LR");
        respuesta.setHora(LocalDateTime.now());
        respuesta.setPasarelaPagos(pasarelaPagoAdapter.getNombrePasarela());
        respuesta.setCortacircuitos(pasarelaPagoAdapter.getEstadoCortacircuitos());
        return respuesta;
    }

    @Override
    public Map<String, Object> metricas() {
        Map<String, Object> datos = new LinkedHashMap<>(MetricasSistema.getInstancia().resumen());
        datos.put("cortacircuitosPagos", pasarelaPagoAdapter.getEstadoCortacircuitos());
        Map<String, Long> outbox = new LinkedHashMap<>();
        outbox.put("pendientes", eventoOutboxServiceAPI.contarPorEstado(Constantes.OUTBOX_PENDIENTE));
        outbox.put("procesados", eventoOutboxServiceAPI.contarPorEstado(Constantes.OUTBOX_PROCESADO));
        outbox.put("error", eventoOutboxServiceAPI.contarPorEstado(Constantes.OUTBOX_ERROR));
        datos.put("outbox", outbox);
        return datos;
    }

    /** Consulta mínima con tiempo límite de 3 s para saber si PostgreSQL responde. */
    private boolean baseDatosResponde() {
        try {
            jdbcTemplate.setQueryTimeout(3);
            Integer uno = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return uno != null && uno == 1;
        } catch (Exception e) {
            logger.severe("salud base_datos=DOWN motivo=\"" + e.getMessage() + "\"");
            return false;
        }
    }
}
