package app.persistence.daos;

import java.util.Set;

public interface IDAO<T> {
    T create(T t);
    T findById(Long id);
    T findByTrackingNumber(String trackingNumber);
    T update(T t);
    Set<T> getAll();
    boolean delete(T t);
}