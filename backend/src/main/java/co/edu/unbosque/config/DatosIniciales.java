package co.edu.unbosque.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import co.edu.unbosque.entity.ConfiguracionTarifa;
import co.edu.unbosque.entity.PoliticaCancelacion;
import co.edu.unbosque.entity.Servicio;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.service.api.ConfiguracionTarifaServiceAPI;
import co.edu.unbosque.service.api.PoliticaCancelacionServiceAPI;
import co.edu.unbosque.service.api.ServicioServiceAPI;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.Constantes;

/**
 * Carga los datos mínimos al iniciar (solo si no existen):
 * políticas de cancelación, servicios incluidos, parámetros de tarifas y el usuario administrador.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(DatosIniciales.class.getName());

    private final PoliticaCancelacionServiceAPI politicaServiceAPI;
    private final ServicioServiceAPI servicioServiceAPI;
    private final ConfiguracionTarifaServiceAPI tarifaServiceAPI;
    private final UsuarioServiceAPI usuarioServiceAPI;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.correo}")
    private String adminCorreo;

    @Value("${admin.clave}")
    private String adminClave;

    public DatosIniciales(PoliticaCancelacionServiceAPI politicaServiceAPI, ServicioServiceAPI servicioServiceAPI,
                          ConfiguracionTarifaServiceAPI tarifaServiceAPI, UsuarioServiceAPI usuarioServiceAPI,
                          PasswordEncoder passwordEncoder) {
        this.politicaServiceAPI = politicaServiceAPI;
        this.servicioServiceAPI = servicioServiceAPI;
        this.tarifaServiceAPI = tarifaServiceAPI;
        this.usuarioServiceAPI = usuarioServiceAPI;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (politicaServiceAPI.getAll().isEmpty()) {
            politicaServiceAPI.save(politica("Flexible", Constantes.POLITICA_FLEXIBLE,
                    "Devolución del 100 % si cancelas con 2 o más días de anticipación; 50 % si es con menos."));
            politicaServiceAPI.save(politica("Estricta", Constantes.POLITICA_ESTRICTA,
                    "Devolución del 50 % si cancelas con 7 o más días de anticipación; sin devolución si es con menos."));
        }
        if (servicioServiceAPI.getAll().isEmpty()) {
            for (String nombre : List.of("Wifi", "Parqueadero", "Piscina", "Cocina equipada", "Aire acondicionado",
                    "Lavadora", "Televisión", "Admite mascotas", "Zona BBQ", "Jacuzzi")) {
                Servicio s = new Servicio();
                s.setNombre(nombre);
                servicioServiceAPI.save(s);
            }
        }
        if (tarifaServiceAPI.getAll().isEmpty()) {
            tarifaServiceAPI.save(tarifa(Constantes.TARIFA_SERVICIO_PORCENTAJE, "10"));
            tarifaServiceAPI.save(tarifa(Constantes.DESCUENTO_SEMANAL_PORCENTAJE, "5"));
            tarifaServiceAPI.save(tarifa(Constantes.NOCHES_DESCUENTO_SEMANAL, "7"));
        }
        boolean hayAdmin = usuarioServiceAPI.getAll().stream()
                .anyMatch(u -> Constantes.ROL_ADMINISTRADOR.equals(u.getRol()));
        if (!hayAdmin) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setCorreo(adminCorreo);
            admin.setHashContrasena(passwordEncoder.encode(adminClave));
            admin.setRol(Constantes.ROL_ADMINISTRADOR);
            admin.setEstado(Constantes.USUARIO_ACTIVO);
            admin.setIntentosFallidos(0);
            admin.setCreadoEn(LocalDateTime.now());
            usuarioServiceAPI.save(admin);
            logger.info("datos_iniciales=admin_creado correo=" + adminCorreo);
        }
    }

    private PoliticaCancelacion politica(String nombre, String tipo, String descripcion) {
        PoliticaCancelacion p = new PoliticaCancelacion();
        p.setNombre(nombre);
        p.setTipo(tipo);
        p.setDescripcion(descripcion);
        return p;
    }

    private ConfiguracionTarifa tarifa(String clave, String valor) {
        ConfiguracionTarifa t = new ConfiguracionTarifa();
        t.setClave(clave);
        t.setValor(new BigDecimal(valor));
        t.setVigenteDesde(LocalDateTime.now());
        return t;
    }
}
