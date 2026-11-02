package co.edu.unbosque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import co.edu.unbosque.dto.LoginRequest;
import co.edu.unbosque.dto.RegistroRequest;
import co.edu.unbosque.dto.ReservaRequest;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.PeriodoDisponibilidad;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.PagoRepository;
import co.edu.unbosque.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.service.api.AuthServiceAPI;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.CredencialesInvalidasException;
import co.edu.unbosque.utils.CuentaBloqueadaException;

/**
 * Pruebas de los ASR del flujo principal contra PostgreSQL (base marketplace_test).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservaServiceImplTest {

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    @Autowired
    private AuthServiceAPI authServiceAPI;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlojamientoRepository alojamientoRepository;

    @Autowired
    private PeriodoDisponibilidadRepository periodoRepository;

    @Autowired
    private PoliticaCancelacionRepository politicaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    /**
     * ADR-02 / HU-27: 10 huéspedes reservan las mismas fechas al mismo tiempo → exactamente 1 confirmada y 9 con 409.
     * Se repite 100 veces para dar confianza de que no hay condiciones de carrera (ASR-01).
     */
    @RepeatedTest(value = 100, name = "concurrencia {currentRepetition}/{totalRepetitions}")
    void debeConfirmarSoloUnaReservaCuandoVariosReservanLasMismasFechas() throws Exception {
        Alojamiento alojamiento = crearAlojamientoActivo();
        int hilos = 10;
        List<Long> huespedes = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            huespedes.add(crearUsuario(Constantes.ROL_HUESPED).getId());
        }
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        for (Long huespedId : huespedes) {
            ejecutor.submit(() -> {
                try {
                    salida.await();
                    ReservaResponse r = reservaServiceAPI.reservar(huespedId, solicitud(alojamiento.getId()),
                            UUID.randomUUID().toString());
                    if (Constantes.RESERVA_CONFIRMADA.equals(r.getEstado())) {
                        exitos.incrementAndGet();
                    }
                } catch (ConflictoException e) {
                    conflictos.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }
        salida.countDown();
        ejecutor.shutdown();
        ejecutor.awaitTermination(60, TimeUnit.SECONDS);
        assertEquals(1, exitos.get(), "Debe existir exactamente una reserva confirmada");
        assertEquals(hilos - 1, conflictos.get(), "Las demás deben recibir 409 por fechas no disponibles");
    }

    /** ADR-03: la misma Idempotency-Key dos veces devuelve la misma reserva y cobra una sola vez. */
    @Test
    void debeCobrarUnaSolaVezConLaMismaIdempotencyKey() {
        Alojamiento alojamiento = crearAlojamientoActivo();
        Long huespedId = crearUsuario(Constantes.ROL_HUESPED).getId();
        String clave = UUID.randomUUID().toString();
        ReservaResponse primera = reservaServiceAPI.reservar(huespedId, solicitud(alojamiento.getId()), clave);
        ReservaResponse segunda = reservaServiceAPI.reservar(huespedId, solicitud(alojamiento.getId()), clave);
        assertEquals(primera.getId(), segunda.getId());
        assertEquals(1, pagoRepository.findByReservaIdOrderByCreadoEnDesc(primera.getId()).size());
    }

    /** ADR-04: tarjeta rechazada → PAGO_FALLIDO y las fechas quedan libres para otra reserva. */
    @Test
    void debeLiberarLasFechasCuandoElPagoEsRechazado() {
        Alojamiento alojamiento = crearAlojamientoActivo();
        Long huesped1 = crearUsuario(Constantes.ROL_HUESPED).getId();
        Long huesped2 = crearUsuario(Constantes.ROL_HUESPED).getId();
        ReservaRequest rechazada = solicitud(alojamiento.getId());
        rechazada.setMetodoPago("pm_card_chargeDeclined");
        ReservaResponse r1 = reservaServiceAPI.reservar(huesped1, rechazada, UUID.randomUUID().toString());
        assertEquals(Constantes.RESERVA_PAGO_FALLIDO, r1.getEstado());
        ReservaResponse r2 = reservaServiceAPI.reservar(huesped2, solicitud(alojamiento.getId()), UUID.randomUUID().toString());
        assertEquals(Constantes.RESERVA_CONFIRMADA, r2.getEstado());
    }

    /** ADR-05: 5 intentos fallidos bloquean la cuenta 15 minutos. */
    @Test
    void debeBloquearLaCuentaTrasCincoIntentosFallidos() {
        String correo = "bloqueo-" + UUID.randomUUID() + "@prueba.com";
        RegistroRequest registro = new RegistroRequest();
        registro.setNombre("Prueba Bloqueo");
        registro.setCorreo(correo);
        registro.setClave("ClaveCorrecta1");
        registro.setRol(Constantes.ROL_HUESPED);
        authServiceAPI.registrar(registro);

        LoginRequest mala = new LoginRequest();
        mala.setCorreo(correo);
        mala.setClave("ClaveIncorrecta");
        for (int i = 0; i < 4; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> authServiceAPI.login(mala));
        }
        assertThrows(CuentaBloqueadaException.class, () -> authServiceAPI.login(mala));

        LoginRequest buena = new LoginRequest();
        buena.setCorreo(correo);
        buena.setClave("ClaveCorrecta1");
        assertThrows(CuentaBloqueadaException.class, () -> authServiceAPI.login(buena));
    }

    // ───────────── datos de prueba ─────────────

    private ReservaRequest solicitud(Long alojamientoId) {
        ReservaRequest r = new ReservaRequest();
        r.setAlojamientoId(alojamientoId);
        r.setFechaLlegada(LocalDate.now().plusDays(10));
        r.setFechaSalida(LocalDate.now().plusDays(13));
        r.setNumHuespedes(2);
        r.setMetodoPago("pm_card_visa");
        return r;
    }

    private Usuario crearUsuario(String rol) {
        Usuario u = new Usuario();
        u.setNombre("Usuario " + rol);
        u.setCorreo(rol.toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com");
        u.setHashContrasena("$2a$10$abcdefghijklmnopqrstuuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012");
        u.setRol(rol);
        u.setEstado(Constantes.USUARIO_ACTIVO);
        u.setIntentosFallidos(0);
        u.setCreadoEn(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    private Alojamiento crearAlojamientoActivo() {
        Alojamiento a = new Alojamiento();
        a.setAnfitrionId(crearUsuario(Constantes.ROL_ANFITRION).getId());
        a.setPoliticaId(politicaRepository.findFirstByTipo(Constantes.POLITICA_FLEXIBLE).orElseThrow().getId());
        a.setTitulo("Casa de prueba");
        a.setDescripcion("Casa de prueba para las pruebas automáticas del sistema.");
        a.setCiudad("Cartagena");
        a.setDireccion("Calle 1 # 2-3");
        a.setTipo("CASA");
        a.setCapacidad(4);
        a.setPrecioNoche(new BigDecimal("200000.00"));
        a.setTarifaLimpieza(new BigDecimal("50000.00"));
        a.setModoReserva(Constantes.MODO_INMEDIATA);
        a.setEstado(Constantes.ALOJAMIENTO_ACTIVA);
        a.setCreadoEn(LocalDateTime.now());
        a = alojamientoRepository.save(a);

        PeriodoDisponibilidad p = new PeriodoDisponibilidad();
        p.setAlojamientoId(a.getId());
        p.setFechaInicio(LocalDate.now().plusDays(1));
        p.setFechaFin(LocalDate.now().plusDays(60));
        p.setEstado(Constantes.PERIODO_DISPONIBLE);
        periodoRepository.save(p);
        return a;
    }
}
