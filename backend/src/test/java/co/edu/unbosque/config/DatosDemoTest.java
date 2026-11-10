package co.edu.unbosque.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
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

/** Catálogo de demostración: se crea una sola vez, con fotos, servicios y disponibilidad. */
class DatosDemoTest {

    private UsuarioRepository usuarios;
    private AlojamientoRepository alojamientos;
    private ImagenRepository imagenes;
    private PeriodoDisponibilidadRepository periodos;
    private ServicioAdicionalRepository adicionales;
    private AlojamientoServicioRepository serviciosIncluidos;
    private DatosDemo datosDemo;
    private final List<Alojamiento> guardados = new ArrayList<>();

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        alojamientos = mock(AlojamientoRepository.class);
        imagenes = mock(ImagenRepository.class);
        periodos = mock(PeriodoDisponibilidadRepository.class);
        adicionales = mock(ServicioAdicionalRepository.class);
        serviciosIncluidos = mock(AlojamientoServicioRepository.class);
        ServicioServiceAPI servicioService = mock(ServicioServiceAPI.class);
        PoliticaCancelacionServiceAPI politicaService = mock(PoliticaCancelacionServiceAPI.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        AtomicLong ids = new AtomicLong(1);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(ids.getAndIncrement());
            return u;
        });
        when(alojamientos.save(any(Alojamiento.class))).thenAnswer(i -> {
            Alojamiento a = i.getArgument(0);
            a.setId(ids.getAndIncrement());
            guardados.add(a);
            return a;
        });
        when(imagenes.save(any(Imagen.class))).thenAnswer(i -> i.getArgument(0));
        when(periodos.save(any(PeriodoDisponibilidad.class))).thenAnswer(i -> i.getArgument(0));
        when(adicionales.save(any(ServicioAdicional.class))).thenAnswer(i -> i.getArgument(0));
        when(serviciosIncluidos.save(any(AlojamientoServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(encoder.encode(anyString())).thenReturn("hash");

        List<Servicio> servicios = new ArrayList<>();
        long n = 1;
        for (String nombre : List.of("Wifi", "Parqueadero", "Piscina", "Cocina equipada", "Aire acondicionado",
                "Lavadora", "Televisión", "Admite mascotas", "Zona BBQ", "Jacuzzi")) {
            Servicio s = new Servicio();
            s.setId(n++);
            s.setNombre(nombre);
            servicios.add(s);
        }
        when(servicioService.getAll()).thenReturn(servicios);
        PoliticaCancelacion flexible = new PoliticaCancelacion();
        flexible.setId(1L);
        flexible.setTipo(Constantes.POLITICA_FLEXIBLE);
        PoliticaCancelacion estricta = new PoliticaCancelacion();
        estricta.setId(2L);
        estricta.setTipo(Constantes.POLITICA_ESTRICTA);
        when(politicaService.getAll()).thenReturn(List.of(flexible, estricta));

        datosDemo = new DatosDemo(usuarios, alojamientos, imagenes, periodos, adicionales, serviciosIncluidos,
                servicioService, politicaService, encoder);
        ReflectionTestUtils.setField(datosDemo, "habilitado", true);
        ReflectionTestUtils.setField(datosDemo, "claveDemo", "ClaveDePrueba1");
    }

    @Test
    void creaElCatalogoCompletoConFotosYDisponibilidad() {
        datosDemo.run();
        verify(usuarios, times(2)).save(any(Usuario.class));
        verify(alojamientos, times(18)).save(any(Alojamiento.class));
        verify(periodos, times(18)).save(any(PeriodoDisponibilidad.class));
        verify(imagenes, org.mockito.Mockito.atLeast(54)).save(any(Imagen.class));
        assertTrue(guardados.stream().allMatch(a -> Constantes.ALOJAMIENTO_ACTIVA.equals(a.getEstado())));
        assertTrue(guardados.stream().allMatch(a -> a.getPoliticaId() != null));
    }

    @Test
    void noRepiteElCatalogoSiYaExisteElAnfitrionDemo() {
        when(usuarios.findByCorreoIgnoreCase(DatosDemo.CORREO_ANFITRION)).thenReturn(Optional.of(new Usuario()));
        datosDemo.run();
        verify(alojamientos, never()).save(any(Alojamiento.class));
    }

    @Test
    void noHaceNadaSiEstaDeshabilitadoOSinClave() {
        ReflectionTestUtils.setField(datosDemo, "habilitado", false);
        datosDemo.run();
        ReflectionTestUtils.setField(datosDemo, "habilitado", true);
        ReflectionTestUtils.setField(datosDemo, "claveDemo", "");
        datosDemo.run();
        verify(usuarios, never()).save(any(Usuario.class));
    }
}
