package app.persistence.daos;

import app.persistence.dtos.GenreDTO;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Genre;

import java.util.List;
import java.util.Set;

public interface IDAO<T> {

    T findById(Long id);
    T update(T t);
    void create(MovieDTO dto);
    void create(GenreDTO dto);  // Tilføjet metode til GenreDTO

    void create(Genre genre);

    List<T> getAllMovies();
}