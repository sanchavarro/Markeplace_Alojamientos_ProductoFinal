package co.edu.unbosque.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import co.edu.unbosque.entity.Servicio;
import co.edu.unbosque.repository.ServicioRepository;
import co.edu.unbosque.service.api.ServicioServiceAPI;
import co.edu.unbosque.utils.GenericServiceImpl;

@Service
public class ServicioServiceImpl extends GenericServiceImpl<Servicio, Long> implements ServicioServiceAPI {

    @Autowired
    private ServicioRepository repository;

    @Override
    public CrudRepository<Servicio, Long> getDao() {
        return repository;
    }
}
