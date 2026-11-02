package co.edu.unbosque.utils;

import java.io.Serializable;
import java.util.List;

/**
 * Operaciones básicas que comparten todos los servicios (convención de la universidad).
 */
public interface GenericServiceAPI<T, ID extends Serializable> {

    T save(T entity);

    void delete(ID id);

    T get(ID id);

    List<T> getAll();
}
