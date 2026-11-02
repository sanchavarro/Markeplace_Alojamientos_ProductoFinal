package co.edu.unbosque.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import co.edu.unbosque.entity.PoliticaCancelacion;
import co.edu.unbosque.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.service.api.PoliticaCancelacionServiceAPI;
import co.edu.unbosque.utils.GenericServiceImpl;

@Service
public class PoliticaCancelacionServiceImpl extends GenericServiceImpl<PoliticaCancelacion, Long> implements PoliticaCancelacionServiceAPI {

    @Autowired
    private PoliticaCancelacionRepository repository;

    @Override
    public CrudRepository<PoliticaCancelacion, Long> getDao() {
        return repository;
    }
}
