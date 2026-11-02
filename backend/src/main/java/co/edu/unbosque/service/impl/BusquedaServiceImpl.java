package co.edu.unbosque.service.impl;

import co.edu.unbosque.dto.AlojamientoDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.AlojamientoResumenDTO;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.repository.AlojamientoRepository;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.BusquedaServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class BusquedaServiceImpl implements BusquedaServiceAPI {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99");

    @Autowired
    private AlojamientoRepository alojamientoRepository;

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    @Value("${paginacion.tamano.defecto}")
    private int tamanoDefecto;

    @Value("${paginacion.tamano.maximo}")
    private int tamanoMaximo;

    /**
     * HU-19 ciudad, fechas y capacidad; HU-20 tipo, rango de precio y servicios;
     * HU-21 solo publicaciones ACTIVAS y disponibles en las fechas pedidas.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AlojamientoResumenDTO> buscar(String ciudad, String tipo, Integer capacidad,
                                                        BigDecimal precioMin, BigDecimal precioMax,
                                                        LocalDate llegada, LocalDate salida, List<Long> servicios,
                                                        Integer pagina, Integer tamano) {
        boolean filtrarFechas = llegada != null || salida != null;
        if (filtrarFechas) {
            Utilidad.validarFechas(llegada, salida);
        }
        BigDecimal minimo = precioMin == null ? BigDecimal.ZERO : precioMin;
        BigDecimal maximo = precioMax == null ? PRECIO_MAXIMO : precioMax;
        if (minimo.compareTo(maximo) > 0) {
            throw new ReglaNegocioException("El precio mínimo no puede ser mayor que el máximo.");
        }
        List<Long> listaServicios = servicios == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(servicios));
        long totalServicios = listaServicios.size();
        if (listaServicios.isEmpty()) {
            listaServicios.add(-1L);
        }
        PageRequest page = PageRequest.of(pagina == null ? 0 : Math.max(pagina, 0),
                Utilidad.normalizarTamanoPagina(tamano, tamanoDefecto, tamanoMaximo));
        return PaginaResponse.desde(alojamientoRepository.buscar(
                ciudad == null ? "" : ciudad.trim(),
                tipo == null ? "" : tipo.trim(),
                capacidad == null || capacidad < 1 ? 1 : capacidad,
                minimo, maximo,
                filtrarFechas,
                filtrarFechas ? llegada : LocalDate.now(),
                filtrarFechas ? salida : LocalDate.now().plusDays(1),
                listaServicios, totalServicios, page), alojamientoServiceAPI::aResumen);
    }

    /** HU-22: comparar de 2 a 3 publicaciones activas. */
    @Override
    @Transactional(readOnly = true)
    public List<AlojamientoDTO> comparar(List<Long> ids) {
        List<Long> unicos = ids == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(ids));
        if (unicos.size() < 2 || unicos.size() > 3) {
            throw new ReglaNegocioException("Puedes comparar entre 2 y 3 alojamientos.");
        }
        List<AlojamientoDTO> resultado = new ArrayList<>();
        for (Alojamiento a : alojamientoRepository.findByIdIn(unicos)) {
            if (Constantes.ALOJAMIENTO_ACTIVA.equals(a.getEstado())) {
                resultado.add(alojamientoServiceAPI.aDTO(a));
            }
        }
        return resultado;
    }
}
