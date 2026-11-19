package co.edu.unbosque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import co.edu.unbosque.PruebaIntegracionBase;
import co.edu.unbosque.dto.ReservaRequest;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.ClaveIdempotencia;
import co.edu.unbosque.repository.ClaveIdempotenciaRepository;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.repository.ReservaRepository;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;

/**
 * Pruebas complementarias de ESC-04 / ASR-01 (concurrencia) y
 * ESC-05 / ASR-02 (idempotencia). El proyecto ya tiene una prueba de
 * 10 solicitudes concurrentes repetida 100 veces: no la duplicamos.
 *
 * Hereda el perfil "test" de PruebaIntegracionBase: usa marketplace_test.
 * No agregar @Transactional: los hilos concurrentes necesitan transacciones reales.
 */
class ReservasConcurrenciaIdempotenciaTest extends PruebaIntegracionBase {

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ClaveIdempotenciaRepository claveIdempotenciaRepository;

    /**
     * Dos solicitudes que se cruzan parcialmente [10,13) y [12,15)
     * llegan a la vez: solo una debe quedar CONFIRMADA.
     * Future.get() propaga los errores inesperados de los hilos al test.
     */
    @Test
    void solicitudesConcurrentesConSolapamientoParcialSoloConfirmanUna() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped1 = crearUsuario(Constantes.ROL_HUESPED).getId();
        Long huesped2 = crearUsuario(Constantes.ROL_HUESPED).getId();
        Long alojamientoId = alojamiento.getId();

        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> tarea1 = () -> intentarReserva(preparados, salida, huesped1,
                    solicitudFechas(alojamientoId, 10, 13));
            Callable<Boolean> tarea2 = () -> intentarReserva(preparados, salida, huesped2,
                    solicitudFechas(alojamientoId, 12, 15));

            Future<Boolean> resultado1 = ejecutor.submit(tarea1);
            Future<Boolean> resultado2 = ejecutor.submit(tarea2);
            assertTrue(preparados.await(10, TimeUnit.SECONDS),
                    "Las dos solicitudes deben prepararse antes de iniciar");
            salida.countDown();

            int confirmadas = (resultado1.get(45, TimeUnit.SECONDS) ? 1 : 0)
                    + (resultado2.get(45, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, confirmadas, "Solo una solicitud puede confirmarse");
            assertEquals(1, contarReservasConfirmadas(alojamientoId),
                    "PostgreSQL debe guardar exactamente una reserva confirmada");
        } finally {
            salida.countDown();
            ejecutor.shutdownNow();
        }
    }

    /** [10,13) y [13,16) no se superponen: el día de salida puede ser otra llegada. */
    @Test
    void reservasConSalidaYLlegadaElMismoDiaSonValidas() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped1 = crearUsuario(Constantes.ROL_HUESPED).getId();
        Long huesped2 = crearUsuario(Constantes.ROL_HUESPED).getId();

        ReservaResponse primera = reservaServiceAPI.reservar(huesped1,
                solicitudFechas(alojamiento.getId(), 10, 13), clave());
        ReservaResponse segunda = reservaServiceAPI.reservar(huesped2,
                solicitudFechas(alojamiento.getId(), 13, 16), clave());

        assertEquals(Constantes.RESERVA_CONFIRMADA, primera.getEstado());
        assertEquals(Constantes.RESERVA_CONFIRMADA, segunda.getEstado());
        assertEquals(2, contarReservasConfirmadas(alojamiento.getId()));
    }

    /** Tres envíos con la misma clave producen una reserva y un solo pago. */
    @Test
    void tresReintentosConLaMismaClaveNoDuplicanReservaNiPago() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaRequest request = solicitud(alojamiento.getId(), 10);
        String idempotencyKey = clave();

        ReservaResponse primera = reservaServiceAPI.reservar(huesped, request, idempotencyKey);
        ReservaResponse segunda = reservaServiceAPI.reservar(huesped, request, idempotencyKey);
        ReservaResponse tercera = reservaServiceAPI.reservar(huesped, request, idempotencyKey);

        assertEquals(Constantes.RESERVA_CONFIRMADA, primera.getEstado());
        assertEquals(primera.getId(), segunda.getId());
        assertEquals(primera.getId(), tercera.getId());
        assertEquals(1, contarReservasConfirmadas(alojamiento.getId()));
        assertEquals(1, pagoRepository.findByReservaIdOrderByCreadoEnDesc(primera.getId()).size());
    }

    /** No se debe permitir que otro usuario reutilice una clave ya procesada. */
    @Test
    void unaClaveDeOtroHuespedEsRechazadaSinCrearOtraReserva() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped1 = crearUsuario(Constantes.ROL_HUESPED).getId();
        Long huesped2 = crearUsuario(Constantes.ROL_HUESPED).getId();
        String idempotencyKey = clave();

        ReservaResponse primera = reservaServiceAPI.reservar(huesped1,
                solicitud(alojamiento.getId(), 10), idempotencyKey);
        assertThrows(ConflictoException.class, () -> reservaServiceAPI.reservar(huesped2,
                solicitud(alojamiento.getId(), 10), idempotencyKey));

        assertEquals(1, contarReservasConfirmadas(alojamiento.getId()));
        assertEquals(1, pagoRepository.findByReservaIdOrderByCreadoEnDesc(primera.getId()).size());
    }

    /** La clave debe venir informada y tener el formato UUID acordado por ADR-03. */
    @Test
    void rechazaClavesAusentesYMalFormadasAntesDeCrearReservas() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaRequest request = solicitud(alojamiento.getId(), 10);

        assertThrows(IllegalArgumentException.class,
                () -> reservaServiceAPI.reservar(huesped, request, null));
        assertThrows(IllegalArgumentException.class,
                () -> reservaServiceAPI.reservar(huesped, request, " "));
        assertThrows(IllegalArgumentException.class,
                () -> reservaServiceAPI.reservar(huesped, request, "clave-no-valida"));

        assertEquals(0, contarReservasConfirmadas(alojamiento.getId()));
    }

    /** Mientras la clave está EN PROCESO, un reintento no debe iniciar otro cobro. */
    @Test
    void claveEnProcesoRechazaReintentosSinCrearReservas() {
        Alojamiento alojamiento = crearAlojamientoActivo(Constantes.POLITICA_FLEXIBLE);
        Long huesped = crearUsuario(Constantes.ROL_HUESPED).getId();
        UUID idempotencyKey = UUID.randomUUID();

        ClaveIdempotencia pendiente = new ClaveIdempotencia();
        pendiente.setClave(idempotencyKey);
        pendiente.setUsuarioId(huesped);
        pendiente.setExpiraEn(LocalDateTime.now().plusHours(1));
        pendiente.setRespuesta(null);
        claveIdempotenciaRepository.save(pendiente);

        assertThrows(ConflictoException.class, () -> reservaServiceAPI.reservar(huesped,
                solicitud(alojamiento.getId(), 10), idempotencyKey.toString()));
        assertEquals(0, contarReservasConfirmadas(alojamiento.getId()));
    }

    private boolean intentarReserva(CountDownLatch preparados, CountDownLatch salida,
                                    Long huespedId, ReservaRequest request) throws InterruptedException {
        preparados.countDown();
        salida.await();
        try {
            ReservaResponse respuesta = reservaServiceAPI.reservar(huespedId, request, clave());
            assertEquals(Constantes.RESERVA_CONFIRMADA, respuesta.getEstado(),
                    "Una reserva que no entra en conflicto debe confirmarse");
            return true;
        } catch (ConflictoException esperado) {
            return false;
        }
    }

    private ReservaRequest solicitudFechas(Long alojamientoId, int llegada, int salida) {
        ReservaRequest request = solicitud(alojamientoId, llegada);
        request.setFechaLlegada(LocalDate.now().plusDays(llegada));
        request.setFechaSalida(LocalDate.now().plusDays(salida));
        return request;
    }

    private long contarReservasConfirmadas(Long alojamientoId) {
        return StreamSupport.stream(reservaRepository.findAll().spliterator(), false)
                .filter(reserva -> alojamientoId.equals(reserva.getAlojamientoId()))
                .filter(reserva -> Constantes.RESERVA_CONFIRMADA.equals(reserva.getEstado()))
                .count();
    }
}
