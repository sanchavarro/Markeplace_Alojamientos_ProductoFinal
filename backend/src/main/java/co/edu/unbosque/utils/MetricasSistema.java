package co.edu.unbosque.utils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PATRÓN SINGLETON + observabilidad (HU-49, T8 Monitor).
 * Una única instancia en toda la aplicación guarda contadores de peticiones, errores, reservas y pagos.
 * Se consulta en GET /salud/metricas.
 */
public final class MetricasSistema {

    private static volatile MetricasSistema instancia;

    private final LocalDateTime iniciadoEn = LocalDateTime.now();
    private final AtomicLong peticiones = new AtomicLong();
    private final AtomicLong errores = new AtomicLong();
    private final AtomicLong tiempoTotalMs = new AtomicLong();
    private final AtomicLong reservasConfirmadas = new AtomicLong();
    private final AtomicLong pagosRechazados = new AtomicLong();
    private final AtomicLong conflictosReserva = new AtomicLong();
    private final AtomicLong respuestasIdempotentes = new AtomicLong();
    private final Map<Integer, AtomicLong> porCodigoHttp = new ConcurrentHashMap<>();

    private MetricasSistema() {
    }

    public static MetricasSistema getInstancia() {
        if (instancia == null) {
            synchronized (MetricasSistema.class) {
                if (instancia == null) {
                    instancia = new MetricasSistema();
                }
            }
        }
        return instancia;
    }

    public void registrarPeticion(int codigoHttp, long duracionMs) {
        peticiones.incrementAndGet();
        tiempoTotalMs.addAndGet(duracionMs);
        porCodigoHttp.computeIfAbsent(codigoHttp, c -> new AtomicLong()).incrementAndGet();
    }

    public void registrarError() {
        errores.incrementAndGet();
    }

    public void registrarReservaConfirmada() {
        reservasConfirmadas.incrementAndGet();
    }

    public void registrarPagoRechazado() {
        pagosRechazados.incrementAndGet();
    }

    public void registrarConflictoReserva() {
        conflictosReserva.incrementAndGet();
    }

    public void registrarRespuestaIdempotente() {
        respuestasIdempotentes.incrementAndGet();
    }

    public Map<String, Object> resumen() {
        Map<String, Object> datos = new LinkedHashMap<>();
        long total = peticiones.get();
        datos.put("iniciadoEn", iniciadoEn.toString());
        datos.put("peticiones", total);
        datos.put("tiempoPromedioMs", total == 0 ? 0 : tiempoTotalMs.get() / total);
        datos.put("erroresInternos", errores.get());
        datos.put("reservasConfirmadas", reservasConfirmadas.get());
        datos.put("pagosRechazados", pagosRechazados.get());
        datos.put("conflictosDeReserva", conflictosReserva.get());
        datos.put("respuestasIdempotentes", respuestasIdempotentes.get());
        Map<String, Long> codigos = new LinkedHashMap<>();
        porCodigoHttp.forEach((k, v) -> codigos.put(String.valueOf(k), v.get()));
        datos.put("porCodigoHttp", codigos);
        return datos;
    }
}
