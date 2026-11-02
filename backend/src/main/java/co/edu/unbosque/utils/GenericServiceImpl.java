package co.edu.unbosque.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

/**
 * Implementación genérica del CRUD. Cada servicio indica su repositorio en getDao() (PAT-03 Repositorio).
 */
@Service
public abstract class GenericServiceImpl<T, ID extends Serializable> implements GenericServiceAPI<T, ID> {

    @Override
    public T save(T entity) {
        return getDao().save(entity);
    }

    @Override
    public void delete(ID id) {
        getDao().deleteById(id);
    }

    @Override
    public T get(ID id) {
        Optional<T> obj = getDao().findById(id);
        return obj.orElse(null);
    }

    @Override
    public List<T> getAll() {
        List<T> resultList = new ArrayList<>();
        getDao().findAll().forEach(resultList::add);
        return resultList;
    }

    public abstract CrudRepository<T, ID> getDao();
}
