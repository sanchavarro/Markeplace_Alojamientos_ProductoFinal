package co.edu.unbosque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import co.edu.unbosque.dto.ReservaRequest;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.PeriodoDisponibilidad;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.utils.Constantes;

/**
 * Base de las pruebas de integración contra PostgreSQL (base marketplace_test, perfil "test").
 * Tiene ayudas para crear usuarios, alojamientos activos y solicitudes de reserva.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class PruebaIntegracionBase {

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected AlojamientoRepository alojamientoRepository;

    @Autowired
    protected PeriodoDisponibilidadRepository periodoRepository;

    @Autowired
    protected PoliticaCancelacionRepository politicaRepository;

    protected Usuario crearUsuario(String rol) {
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

    protected Usuario administrador() {
        return usuarioRepository.findByCorreoIgnoreCase("admin@marketplace.com").orElseThrow();
    }

    protected Alojamiento crearAlojamientoActivo(String politica) {
        Alojamiento a = new Alojamiento();
        a.setAnfitrionId(crearUsuario(Constantes.ROL_ANFITRION).getId());
        a.setPoliticaId(politicaRepository.findFirstByTipo(politica).orElseThrow().getId());
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

    protected ReservaRequest solicitud(Long alojamientoId, int diasHastaLlegada) {
        ReservaRequest r = new ReservaRequest();
        r.setAlojamientoId(alojamientoId);
        r.setFechaLlegada(LocalDate.now().plusDays(diasHastaLlegada));
        r.setFechaSalida(LocalDate.now().plusDays(diasHastaLlegada + 3));
        r.setNumHuespedes(2);
        r.setMetodoPago("pm_card_visa");
        return r;
    }

    protected String clave() {
        return UUID.randomUUID().toString();
    }
}
