package co.edu.unbosque.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PATRÓN CORTACIRCUITOS (PAT-05) con tiempo límite (ADR-04, tácticas T6 y T7).
 *
 * CERRADO     → las llamadas pasan normalmente.
 * ABIERTO     → después de N fallos seguidos, se responde de inmediato "servicio no disponible"
 *               sin llamar a la pasarela, durante X segundos.
 * SEMIABIERTO → pasado ese tiempo se deja pasar una llamada de prueba; si funciona se cierra,
 *               si falla se vuelve a abrir.
 *
 * Además, ninguna llamada puede tardar más de pagos.timeout.segundos (10 s).
 */
@Component
public class Cortacircuitos {

    public enum Estado { CERRADO, ABIERTO, SEMIABIERTO }

    private static final Logger logger = Logger.getLogger(Cortacircuitos.class.getName());

    private final int fallosParaAbrir;
    private final long esperaMs;
    private final long timeoutSegundos;
    private final ExecutorService ejecutor = Executors.newCachedThreadPool();

    private Estado estado = Estado.CERRADO;
    private int fallosSeguidos = 0;
    private long abiertoDesde = 0;

    public Cortacircuitos(@Value("${pagos.cortacircuitos.fallos}") int fallosParaAbrir,
                          @Value("${pagos.cortacircuitos.espera.segundos}") long esperaSegundos,
                          @Value("${pagos.timeout.segundos}") long timeoutSegundos) {
        this.fallosParaAbrir = fallosParaAbrir;
        this.esperaMs = esperaSegundos * 1000;
        this.timeoutSegundos = timeoutSegundos;
    }

    public <T> T ejecutar(Supplier<T> accion) {
        verificarSiPuedePasar();
        CompletableFuture<T> futuro = CompletableFuture.supplyAsync(accion, ejecutor);
        try {
            T resultado = futuro.get(timeoutSegundos, TimeUnit.SECONDS);
            registrarExito();
            return resultado;
        } catch (TimeoutException e) {
            futuro.cancel(true);
            registrarFallo("tiempo límite de " + timeoutSegundos + " s superado");
            throw new ServicioNoDisponibleException(
                    "La pasarela de pagos no respondió a tiempo. Intenta de nuevo en unos minutos.");
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof PagoRechazadoException rechazo) {
                // Un rechazo de tarjeta es una respuesta válida de la pasarela: no cuenta como fallo.
                registrarExito();
                throw rechazo;
            }
            registrarFallo(causa == null ? "error desconocido" : causa.getMessage());
            throw new ServicioNoDisponibleException(
                    "La pasarela de pagos no está disponible en este momento. Intenta más tarde.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("La operación de pago fue interrumpida.");
        }
    }

    private synchronized void verificarSiPuedePasar() {
        if (estado == Estado.ABIERTO) {
            if (System.currentTimeMillis() - abiertoDesde >= esperaMs) {
                estado = Estado.SEMIABIERTO;
                logger.info("cortacircuitos=SEMIABIERTO accion=llamada_de_prueba");
            } else {
                throw new ServicioNoDisponibleException(
                        "El servicio de pagos está temporalmente suspendido. Intenta de nuevo en unos segundos.");
            }
        }
    }

    private synchronized void registrarExito() {
        if (estado != Estado.CERRADO) {
            logger.info("cortacircuitos=CERRADO motivo=llamada_exitosa");
        }
        estado = Estado.CERRADO;
        fallosSeguidos = 0;
    }

    private synchronized void registrarFallo(String motivo) {
        fallosSeguidos++;
        logger.warning("cortacircuitos_fallo=" + fallosSeguidos + " motivo=\"" + motivo + "\"");
        if (estado == Estado.SEMIABIERTO || fallosSeguidos >= fallosParaAbrir) {
            estado = Estado.ABIERTO;
            abiertoDesde = System.currentTimeMillis();
            logger.warning("cortacircuitos=ABIERTO espera_segundos=" + (esperaMs / 1000));
        }
    }

    public synchronized Estado getEstado() {
        return estado;
    }

    public synchronized int getFallosSeguidos() {
        return fallosSeguidos;
    }
}
