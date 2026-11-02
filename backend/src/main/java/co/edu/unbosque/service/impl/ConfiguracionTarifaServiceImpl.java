package co.edu.unbosque.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.entity.ConfiguracionTarifa;
import co.edu.unbosque.repository.ConfiguracionTarifaRepository;
import co.edu.unbosque.service.api.ConfiguracionTarifaServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.GenericServiceImpl;
import co.edu.unbosque.utils.ResourceNotFoundException;
import co.edu.unbosque.utils.Utilidad;

@Service
public class ConfiguracionTarifaServiceImpl extends GenericServiceImpl<ConfiguracionTarifa, Long>
        implements ConfiguracionTarifaServiceAPI {

    private static final List<String> CLAVES_VALIDAS = List.of(Constantes.TARIFA_SERVICIO_PORCENTAJE,
            Constantes.DESCUENTO_SEMANAL_PORCENTAJE, Constantes.NOCHES_DESCUENTO_SEMANAL);

    @Autowired
    private ConfiguracionTarifaRepository configuracionTarifaRepository;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Override
    public CrudRepository<ConfiguracionTarifa, Long> getDao() {
        return configuracionTarifaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal valor(String clave, BigDecimal porDefecto) {
        return configuracionTarifaRepository.findByClave(clave)
                .map(ConfiguracionTarifa::getValor)
                .orElse(porDefecto);
    }

    @Override
    @Transactional
    public ConfiguracionTarifa actualizar(Long adminId, String clave, BigDecimal valor) {
        String claveNormalizada = clave.trim().toUpperCase();
        if (!CLAVES_VALIDAS.contains(claveNormalizada)) {
            throw new ResourceNotFoundException("Parámetro de tarifa no existe. Válidos: " + CLAVES_VALIDAS);
        }
        ConfiguracionTarifa tarifa = configuracionTarifaRepository.findByClave(claveNormalizada)
                .orElseGet(ConfiguracionTarifa::new);
        BigDecimal anterior = tarifa.getValor();
        tarifa.setClave(claveNormalizada);
        tarifa.setValor(valor);
        tarifa.setVigenteDesde(LocalDateTime.now());
        tarifa = configuracionTarifaRepository.save(tarifa);
        eventoAuditoriaServiceAPI.registrar(adminId, "CONFIGURACION_TARIFA", tarifa.getId(), "ACTUALIZAR_TARIFA",
                Constantes.AUDITORIA_EXITO, Utilidad.detalle("clave", claveNormalizada, "anterior", anterior, "nuevo", valor));
        return tarifa;
    }
}
