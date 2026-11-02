package co.edu.unbosque.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.CotizacionRequest;
import co.edu.unbosque.dto.CotizacionResponse;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.ServicioAdicional;
import co.edu.unbosque.repository.ServicioAdicionalRepository;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.ConfiguracionTarifaServiceAPI;
import co.edu.unbosque.service.api.CotizacionServiceAPI;
import co.edu.unbosque.service.api.DisponibilidadServiceAPI;
import co.edu.unbosque.utils.ConflictoException;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.ReglaNegocioException;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

/**
 * Cálculo de la cotización:
 * precioBase = noches × precioNoche
 * serviciosAdicionales = suma de los servicios elegidos
 * tarifaServicio = TARIFA_SERVICIO_PORCENTAJE % de (precioBase + limpieza + adicionales)
 * descuento = DESCUENTO_SEMANAL_PORCENTAJE % del precioBase si la estadía es de NOCHES_DESCUENTO_SEMANAL o más
 * total = precioBase + limpieza + adicionales + tarifaServicio − descuento
 */
@Service
public class CotizacionServiceImpl implements CotizacionServiceAPI {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    @Autowired
    private DisponibilidadServiceAPI disponibilidadServiceAPI;

    @Autowired
    private ServicioAdicionalRepository servicioAdicionalRepository;

    @Autowired
    private ConfiguracionTarifaServiceAPI configuracionTarifaServiceAPI;

    @Override
    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(CotizacionRequest request) {
        Alojamiento alojamiento = alojamientoServiceAPI.obtener(request.getAlojamientoId());
        if (!Constantes.ALOJAMIENTO_ACTIVA.equals(alojamiento.getEstado())) {
            throw new ResourceNotFoundException("Alojamiento no encontrado.");
        }
        CotizacionResponse cotizacion = calcular(alojamiento, request.getFechaLlegada(), request.getFechaSalida(),
                request.getNumHuespedes(), request.getServiciosAdicionales());
        if (!disponibilidadServiceAPI.estaDisponible(alojamiento.getId(), request.getFechaLlegada(),
                request.getFechaSalida())) {
            throw new ConflictoException("El alojamiento no está disponible en esas fechas.");
        }
        return cotizacion;
    }

    @Override
    public CotizacionResponse calcular(Alojamiento alojamiento, LocalDate llegada, LocalDate salida,
                                       Integer numHuespedes, List<Long> serviciosAdicionales) {
        Utilidad.validarFechas(llegada, salida);
        if (numHuespedes == null || numHuespedes < 1) {
            throw new ReglaNegocioException("Debe haber al menos un huésped.");
        }
        if (numHuespedes > alojamiento.getCapacidad()) {
            throw new ReglaNegocioException("El alojamiento admite máximo " + alojamiento.getCapacidad() + " huéspedes.");
        }
        int noches = (int) Utilidad.calcularNoches(llegada, salida);
        BigDecimal precioBase = alojamiento.getPrecioNoche().multiply(BigDecimal.valueOf(noches));
        BigDecimal limpieza = alojamiento.getTarifaLimpieza() == null ? BigDecimal.ZERO : alojamiento.getTarifaLimpieza();

        List<ServicioAdicional> elegidos = new ArrayList<>();
        BigDecimal adicionales = BigDecimal.ZERO;
        if (serviciosAdicionales != null) {
            for (Long id : new LinkedHashSet<>(serviciosAdicionales)) {
                ServicioAdicional s = servicioAdicionalRepository.findById(id)
                        .filter(x -> x.getAlojamientoId().equals(alojamiento.getId()))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "El servicio adicional " + id + " no pertenece a este alojamiento."));
                elegidos.add(s);
                adicionales = adicionales.add(s.getPrecio());
            }
        }

        BigDecimal porcentajeServicio = configuracionTarifaServiceAPI.valor(Constantes.TARIFA_SERVICIO_PORCENTAJE,
                BigDecimal.TEN);
        BigDecimal subtotal = precioBase.add(limpieza).add(adicionales);
        BigDecimal tarifaServicio = subtotal.multiply(porcentajeServicio).divide(CIEN, 2, RoundingMode.HALF_UP);

        BigDecimal nochesDescuento = configuracionTarifaServiceAPI.valor(Constantes.NOCHES_DESCUENTO_SEMANAL,
                BigDecimal.valueOf(7));
        BigDecimal descuento = BigDecimal.ZERO;
        if (BigDecimal.valueOf(noches).compareTo(nochesDescuento) >= 0) {
            BigDecimal porcentajeDescuento = configuracionTarifaServiceAPI.valor(
                    Constantes.DESCUENTO_SEMANAL_PORCENTAJE, BigDecimal.ZERO);
            descuento = precioBase.multiply(porcentajeDescuento).divide(CIEN, 2, RoundingMode.HALF_UP);
        }
        BigDecimal total = subtotal.add(tarifaServicio).subtract(descuento).setScale(2, RoundingMode.HALF_UP);

        CotizacionResponse c = new CotizacionResponse();
        c.setAlojamientoId(alojamiento.getId());
        c.setFechaLlegada(llegada);
        c.setFechaSalida(salida);
        c.setNumHuespedes(numHuespedes);
        c.setNoches(noches);
        c.setPrecioNoche(alojamiento.getPrecioNoche());
        c.setPrecioBase(precioBase.setScale(2, RoundingMode.HALF_UP));
        c.setTarifaLimpieza(limpieza.setScale(2, RoundingMode.HALF_UP));
        c.setServiciosAdicionales(adicionales.setScale(2, RoundingMode.HALF_UP));
        c.setTarifaServicio(tarifaServicio);
        c.setDescuento(descuento);
        c.setTotal(total);
        c.setMoneda("COP");
        c.setDetalleServicios(elegidos);
        return c;
    }
}
