package co.edu.unbosque.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.AlojamientoServicio;
import co.edu.unbosque.entity.Imagen;
import co.edu.unbosque.entity.PeriodoDisponibilidad;
import co.edu.unbosque.entity.PoliticaCancelacion;
import co.edu.unbosque.entity.Servicio;
import co.edu.unbosque.entity.ServicioAdicional;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.repository.AlojamientoServicioRepository;
import co.edu.unbosque.repository.ImagenRepository;
import co.edu.unbosque.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.repository.ServicioAdicionalRepository;
import co.edu.unbosque.repository.UsuarioRepository;
import co.edu.unbosque.service.api.PoliticaCancelacionServiceAPI;
import co.edu.unbosque.service.api.ServicioServiceAPI;
import co.edu.unbosque.utils.Constantes;

/**
 * Catálogo de demostración: crea (una sola vez) un anfitrión y un huésped de prueba y 18 alojamientos
 * ACTIVOS en distintas ciudades de Colombia, cada uno con fotos propias, servicios, servicios adicionales
 * y disponibilidad para los próximos 12 meses.
 *
 * Se desactiva con datos.demo=false (o la variable DATOS_DEMO=false). Las pruebas lo desactivan.
 * La clave de las cuentas demo se toma de datos.demo.clave (variable DATOS_DEMO_CLAVE).
 */
@Component
@Order(2)
public class DatosDemo implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(DatosDemo.class.getName());

    static final String CORREO_ANFITRION = "anfitrion.demo@marketplace.com";
    static final String CORREO_HUESPED = "huesped.demo@marketplace.com";

    private final UsuarioRepository usuarioRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final ImagenRepository imagenRepository;
    private final PeriodoDisponibilidadRepository periodoRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;
    private final AlojamientoServicioRepository alojamientoServicioRepository;
    private final ServicioServiceAPI servicioServiceAPI;
    private final PoliticaCancelacionServiceAPI politicaServiceAPI;
    private final PasswordEncoder passwordEncoder;

    @Value("${datos.demo:true}")
    private boolean habilitado;

    /** Clave de las cuentas demo (propiedad datos.demo.clave / variable DATOS_DEMO_CLAVE), nunca en el código. */
    @Value("${datos.demo.clave:}")
    private String claveDemo;

    public DatosDemo(UsuarioRepository usuarioRepository, AlojamientoRepository alojamientoRepository,
                     ImagenRepository imagenRepository, PeriodoDisponibilidadRepository periodoRepository,
                     ServicioAdicionalRepository servicioAdicionalRepository,
                     AlojamientoServicioRepository alojamientoServicioRepository,
                     ServicioServiceAPI servicioServiceAPI, PoliticaCancelacionServiceAPI politicaServiceAPI,
                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.alojamientoRepository = alojamientoRepository;
        this.imagenRepository = imagenRepository;
        this.periodoRepository = periodoRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
        this.alojamientoServicioRepository = alojamientoServicioRepository;
        this.servicioServiceAPI = servicioServiceAPI;
        this.politicaServiceAPI = politicaServiceAPI;
        this.passwordEncoder = passwordEncoder;
    }

    /** Datos de un alojamiento de demostración. */
    private record Demo(String titulo, String ciudad, String direccion, String tipo, int capacidad, int precio,
                        int limpieza, String modo, String politica, String descripcion, String reglas,
                        List<String> servicios, Map<String, Integer> adicionales, List<String> fotos) {
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || claveDemo == null || claveDemo.isBlank()
                || usuarioRepository.findByCorreoIgnoreCase(CORREO_ANFITRION).isPresent()) {
            return;
        }
        Usuario anfitrion = usuario("Valentina Ríos", CORREO_ANFITRION, Constantes.ROL_ANFITRION, "+57 310 555 0101");
        usuario("Mateo Gómez", CORREO_HUESPED, Constantes.ROL_HUESPED, "+57 320 555 0202");

        Map<String, Long> servicios = new HashMap<>();
        for (Servicio s : servicioServiceAPI.getAll()) {
            servicios.put(s.getNombre(), s.getId());
        }
        Map<String, Long> politicas = new HashMap<>();
        for (PoliticaCancelacion p : politicaServiceAPI.getAll()) {
            politicas.put(p.getTipo(), p.getId());
        }

        int creados = 0;
        for (Demo d : catalogo()) {
            Alojamiento a = new Alojamiento();
            a.setAnfitrionId(anfitrion.getId());
            a.setPoliticaId(politicas.get(d.politica()));
            a.setTitulo(d.titulo());
            a.setDescripcion(d.descripcion());
            a.setReglas(d.reglas());
            a.setCiudad(d.ciudad());
            a.setDireccion(d.direccion());
            a.setTipo(d.tipo());
            a.setCapacidad(d.capacidad());
            a.setPrecioNoche(BigDecimal.valueOf(d.precio()));
            a.setTarifaLimpieza(BigDecimal.valueOf(d.limpieza()));
            a.setModoReserva(d.modo());
            a.setEstado(Constantes.ALOJAMIENTO_ACTIVA);
            a.setCreadoEn(LocalDateTime.now());
            a = alojamientoRepository.save(a);

            for (int i = 0; i < d.fotos().size(); i++) {
                Imagen img = new Imagen();
                img.setAlojamientoId(a.getId());
                img.setUrl("https://images.unsplash.com/photo-" + d.fotos().get(i) + "?auto=format&fit=crop&w=1600&q=80");
                img.setOrden(i);
                imagenRepository.save(img);
            }
            for (String nombre : d.servicios()) {
                Long id = servicios.get(nombre);
                if (id != null) {
                    alojamientoServicioRepository.save(new AlojamientoServicio(a.getId(), id));
                }
            }
            for (Map.Entry<String, Integer> extra : d.adicionales().entrySet()) {
                ServicioAdicional sa = new ServicioAdicional();
                sa.setAlojamientoId(a.getId());
                sa.setNombre(extra.getKey());
                sa.setPrecio(BigDecimal.valueOf(extra.getValue()));
                servicioAdicionalRepository.save(sa);
            }
            PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
            periodo.setAlojamientoId(a.getId());
            periodo.setFechaInicio(LocalDate.now());
            periodo.setFechaFin(LocalDate.now().plusMonths(12));
            periodo.setEstado(Constantes.PERIODO_DISPONIBLE);
            periodoRepository.save(periodo);
            creados++;
        }
        logger.info("datos_demo=catalogo_creado alojamientos=" + creados + " anfitrion=" + CORREO_ANFITRION
                + " huesped=" + CORREO_HUESPED);
    }

    private Usuario usuario(String nombre, String correo, String rol, String telefono) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setHashContrasena(passwordEncoder.encode(claveDemo));
        u.setRol(rol);
        u.setEstado(Constantes.USUARIO_ACTIVO);
        u.setTelefono(telefono);
        u.setIntentosFallidos(0);
        u.setCorreoVerificado(true);
        u.setCreadoEn(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    private static final String FLEX = Constantes.POLITICA_FLEXIBLE;
    private static final String ESTR = Constantes.POLITICA_ESTRICTA;
    private static final String INM = Constantes.MODO_INMEDIATA;
    private static final String APR = Constantes.MODO_APROBACION;
    private static final String REGLAS = "Llegada desde las 3:00 p. m. y salida hasta las 11:00 a. m.\nNo se permiten fiestas ni eventos.\nRespeta el descanso de los vecinos después de las 10:00 p. m.";

    private List<Demo> catalogo() {
        return List.of(
            new Demo("Casa colonial con piscina en el Centro Histórico", "Cartagena", "Calle del Cuartel 36-21", "CASA", 8, 780000, 120000, INM, ESTR,
                "Casa colonial restaurada a pocas cuadras de la Torre del Reloj. Patio interior con piscina, techos altos, cuatro habitaciones con baño privado y terraza con vista a las cúpulas del centro amurallado.",
                REGLAS, List.of("Wifi", "Piscina", "Aire acondicionado", "Cocina equipada", "Televisión"),
                Map.of("Desayuno típico", 45000, "Traslado desde el aeropuerto", 90000),
                List.of("1613490493576-7fde63acd811", "1600607687939-ce8a6c25118c", "1600566753190-17f0baa2a6c3", "1505693416388-ac5ce068fe85", "1600121848594-d8644e57abab")),
            new Demo("Apartamento frente al mar en Bocagrande", "Cartagena", "Carrera 1 # 9-150, Bocagrande", "APARTAMENTO", 4, 360000, 60000, INM, FLEX,
                "Apartamento en piso alto con balcón y vista directa al mar Caribe. A una cuadra de la playa y de restaurantes. Edificio con piscina y portería 24 horas.",
                REGLAS, List.of("Wifi", "Aire acondicionado", "Piscina", "Parqueadero", "Televisión"),
                Map.of("Kit de playa", 25000),
                List.of("1507525428034-b723cf961d3e", "1522708323590-d24dbb6b0267", "1540518614846-7eded433c457", "1556020685-ae41abfc9365")),
            new Demo("Loft de diseño en El Poblado", "Medellín", "Calle 10A # 36-50, El Poblado", "APARTAMENTO", 2, 240000, 40000, INM, FLEX,
                "Loft luminoso con doble altura, cocina abierta y escritorio para trabajar. A pasos del Parque Lleras y de los mejores cafés de la ciudad de la eterna primavera.",
                REGLAS, List.of("Wifi", "Cocina equipada", "Lavadora", "Televisión"),
                Map.of("Limpieza adicional", 50000),
                List.of("1502672260266-1c1ef2d93688", "1560448204-e02f11c3d0e2", "1522771739844-6a9f6d5f14af", "1556911220-bff31c812dba")),
            new Demo("Casa campestre con jacuzzi en Llanogrande", "Rionegro", "Vereda Llanogrande, km 3", "CASA", 10, 650000, 100000, APR, ESTR,
                "Casa de campo rodeada de jardines a 20 minutos del aeropuerto José María Córdova. Chimenea, jacuzzi exterior, zona BBQ y amplias zonas verdes para compartir en familia.",
                REGLAS, List.of("Wifi", "Jacuzzi", "Zona BBQ", "Parqueadero", "Cocina equipada", "Admite mascotas"),
                Map.of("Leña para chimenea", 35000, "Chef privado (cena)", 250000),
                List.of("1600585154340-be6161a56a0c", "1600210492486-724fe5c67fb0", "1600573472592-401b489a3cdc", "1505691938895-1758d7feb511", "1600489000022-c2086d79f9d4")),
            new Demo("Apartamento moderno en Chapinero Alto", "Bogotá", "Calle 64 # 4-20, Chapinero", "APARTAMENTO", 3, 210000, 35000, INM, FLEX,
                "Apartamento acogedor con vista a los cerros orientales, cerca de la Zona G y de la ciclovía. Ideal para viajes de trabajo o una escapada cultural en la capital.",
                REGLAS, List.of("Wifi", "Cocina equipada", "Lavadora", "Televisión", "Parqueadero"),
                Map.of("Desayuno continental", 30000),
                List.of("1560185007-cde436f6a4d0", "1493809842364-78817add7ffb", "1554995207-c18c203602cb", "1618773928121-c32242e63f39")),
            new Demo("Casa de La Candelaria con terraza", "Bogotá", "Carrera 2 # 12-48, La Candelaria", "CASA", 6, 330000, 50000, APR, FLEX,
                "Casa republicana en el barrio más histórico de Bogotá, a pasos del Museo del Oro y del Chorro de Quevedo. Terraza con vista a Monserrate.",
                REGLAS, List.of("Wifi", "Cocina equipada", "Televisión"),
                Map.of("Tour guiado por el centro", 80000),
                List.of("1518780664697-55e3ad937233", "1586023492125-27b2c045efd7", "1484154218962-a197022b5858", "1582719478250-c89cae4dc85b")),
            new Demo("Cabaña en el bosque de niebla", "Salento", "Vereda Palestina, vía al Valle de Cocora", "CABANA", 4, 280000, 40000, INM, FLEX,
                "Cabaña de madera entre palmas de cera y bosque de niebla, a 10 minutos del Valle de Cocora. Desayuno con café de la finca y miradores para ver el amanecer.",
                REGLAS, List.of("Wifi", "Cocina equipada", "Admite mascotas", "Zona BBQ"),
                Map.of("Cabalgata al Valle de Cocora", 120000, "Tour de café", 70000),
                List.of("1449158743715-0a90ebb6d2d8", "1510798831971-661eb04b3739", "1470770841072-f978cf4d019e", "1522771739844-6a9f6d5f14af")),
            new Demo("Finca cafetera con vista a las montañas", "Manizales", "Vía al Nevado del Ruiz, km 8", "FINCA", 12, 520000, 90000, APR, ESTR,
                "Finca tradicional en plena zona cafetera, con corredores, hamacas, piscina y recorridos por el cultivo. Perfecta para grupos grandes y celebraciones familiares.",
                REGLAS, List.of("Wifi", "Piscina", "Zona BBQ", "Parqueadero", "Cocina equipada", "Admite mascotas"),
                Map.of("Almuerzo típico para el grupo", 180000),
                List.of("1570129477492-45c003edd2be", "1600047509807-ba8f99d2cdde", "1600566753190-17f0baa2a6c3", "1540518614846-7eded433c457")),
            new Demo("Villa frente al mar en Rodadero", "Santa Marta", "Carrera 1 # 7-45, El Rodadero", "CASA", 8, 690000, 110000, INM, ESTR,
                "Villa con piscina privada y salida directa a la playa. Cuatro habitaciones con aire acondicionado y una terraza para ver el atardecer sobre la bahía.",
                REGLAS, List.of("Wifi", "Piscina", "Aire acondicionado", "Parqueadero", "Zona BBQ"),
                Map.of("Pasadía en lancha a Playa Blanca", 150000),
                List.of("1613977257363-707ba9348227", "1499793983690-e29da59ef1c2", "1600607687939-ce8a6c25118c", "1631049307264-da0ec9d70304", "1566073771259-6a8506099945")),
            new Demo("Glamping con vista a la Sierra Nevada", "Minca", "Vereda El Campano, Minca", "GLAMPING", 2, 260000, 30000, INM, FLEX,
                "Domo con cama king, ducha de agua caliente y deck privado entre la selva de la Sierra Nevada. Cascadas y avistamiento de aves a pocos minutos.",
                "Llegada desde las 2:00 p. m.\nZona libre de humo.\nNo dejar alimentos fuera del domo.", List.of("Wifi", "Jacuzzi"),
                Map.of("Cena a la luz de las velas", 140000),
                List.of("1504280390367-361c6d9f38f4", "1470770841072-f978cf4d019e", "1587061949409-02df41d5e562")),
            new Demo("Casa en piedra con jardín en Villa de Leyva", "Villa de Leyva", "Calle 12 # 8-30", "CASA", 6, 400000, 60000, INM, FLEX,
                "Casa colonial con muros de piedra y patio florido, a tres cuadras de la plaza principal empedrada más grande de Colombia. Chimenea para las noches frescas.",
                REGLAS, List.of("Wifi", "Cocina equipada", "Parqueadero", "Admite mascotas"),
                Map.of("Alquiler de bicicletas", 40000),
                List.of("1564013799919-ab600027ffc6", "1600210492486-724fe5c67fb0", "1505693416388-ac5ce068fe85", "1556020685-ae41abfc9365")),
            new Demo("Cabaña frente al embalse de Guatapé", "Guatapé", "Vía a la Piedra del Peñol, km 2", "CABANA", 6, 450000, 70000, APR, ESTR,
                "Cabaña con muelle propio sobre el embalse, kayaks incluidos y vista a la Piedra del Peñol. Ideal para desconectarse entre agua y montaña.",
                REGLAS, List.of("Wifi", "Zona BBQ", "Parqueadero", "Cocina equipada", "Jacuzzi"),
                Map.of("Paseo en lancha", 100000),
                List.of("1542718610-a1d656d1884c", "1587061949409-02df41d5e562", "1510798831971-661eb04b3739", "1505691938895-1758d7feb511")),
            new Demo("Posada nativa en San Andrés", "San Andrés", "Sector San Luis, vía Circunvalar", "HABITACION", 2, 230000, 30000, INM, FLEX,
                "Habitación en posada raizal de madera de colores, a pasos del mar de siete colores. Desayuno isleño incluido y snorkel en la playa del frente.",
                REGLAS, List.of("Wifi", "Aire acondicionado"),
                Map.of("Tour a Johnny Cay", 90000),
                List.of("1519046904884-53103b34b206", "1582719478250-c89cae4dc85b", "1520250497591-112f2f40a3f4")),
            new Demo("Apartamento con piscina en el oeste de Cali", "Cali", "Avenida 4 Oeste # 5-90", "APARTAMENTO", 4, 220000, 35000, INM, FLEX,
                "Apartamento fresco con balcón, piscina del conjunto y vista a los Farallones. Cerca del barrio San Antonio y de las mejores escuelas de salsa.",
                REGLAS, List.of("Wifi", "Piscina", "Aire acondicionado", "Parqueadero", "Lavadora"),
                Map.of("Clase privada de salsa", 90000),
                List.of("1600596542815-ffad4c1539a9", "1560184897-ae75f418493e", "1600489000022-c2086d79f9d4", "1618773928121-c32242e63f39")),
            new Demo("Casa blanca en Barichara", "Barichara", "Calle 6 # 4-12", "CASA", 5, 380000, 55000, APR, FLEX,
                "Casa de tapia pisada en el pueblo más lindo de Colombia. Patio con fuente, hamacas y vista al cañón del río Suárez. Punto de partida del Camino Real a Guane.",
                REGLAS, List.of("Wifi", "Cocina equipada", "Admite mascotas"),
                Map.of("Guía por el Camino Real", 60000),
                List.of("1568605114967-8130f3a36994", "1586023492125-27b2c045efd7", "1540518614846-7eded433c457", "1556911220-bff31c812dba")),
            new Demo("Resort boutique con termales en Paipa", "Paipa", "Vía al Lago Sochagota, km 1", "HABITACION", 2, 310000, 0, INM, ESTR,
                "Suite junior en un hotel boutique con acceso a piscinas termales, spa y vista al Lago Sochagota.",
                REGLAS, List.of("Wifi", "Piscina", "Jacuzzi", "Parqueadero", "Televisión"),
                Map.of("Masaje relajante", 160000),
                List.of("1571896349842-33c89424de2d", "1631049307264-da0ec9d70304", "1551882547-ff40c63fe5fa")),
            new Demo("Casa de descanso con piscina en Melgar", "Melgar", "Condominio Las Palmas, casa 14", "CASA", 9, 480000, 80000, INM, FLEX,
                "Casa de clima cálido en condominio cerrado, con piscina privada, kiosko con BBQ y cancha. El plan perfecto de fin de semana a dos horas de Bogotá.",
                REGLAS, List.of("Wifi", "Piscina", "Zona BBQ", "Parqueadero", "Aire acondicionado", "Televisión"),
                Map.of("Mercado surtido a la llegada", 120000),
                List.of("1580587771525-78b9dba3b914", "1566073771259-6a8506099945", "1560448204-e02f11c3d0e2", "1522771739844-6a9f6d5f14af")),
            new Demo("Eco-villa en la selva cerca a Tayrona", "Palomino", "Playa Palomino, sector La Sirena", "CABANA", 4, 340000, 50000, APR, ESTR,
                "Villa ecológica construida en guadua entre la selva y el mar, a minutos del Parque Tayrona. Energía solar, ducha al aire libre y tubing por el río Palomino.",
                "Llegada desde las 3:00 p. m.\nUsa productos biodegradables.\nNo se permite música a alto volumen.", List.of("Wifi", "Cocina equipada", "Admite mascotas"),
                Map.of("Tubing por el río Palomino", 60000, "Clase de yoga al amanecer", 50000),
                List.of("1537996194471-e657df975ab4", "1507525428034-b723cf961d3e", "1600573472592-401b489a3cdc", "1505693416388-ac5ce068fe85"))
        );
    }
}
