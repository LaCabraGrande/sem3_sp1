package app.persistence.daos;

import java.util.List;

public interface IDAO<T> {
        void create(T entity);
        T findById(Long id);
        void update(T entity);
        void delete(Long id);
        List<T> getAll();
}
