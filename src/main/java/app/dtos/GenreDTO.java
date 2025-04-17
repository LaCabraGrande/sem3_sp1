package app.dtos;

import app.entities.Genre;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GenreDTO {
    private Long id;
    private int genreId;
    private String name;

    private Set<Long> movieIds;
    private Set<String> movieTitles;

    public GenreDTO(Genre genre) {
        this.id = genre.getId();
        this.genreId = genre.getGenreId();
        this.name = genre.getName();

        if (Hibernate.isInitialized(genre.getMovies()) && genre.getMovies() != null) {
            this.movieIds = genre.getMovies().stream()
                    .map(movie -> movie.getId())
                    .collect(Collectors.toSet());

            this.movieTitles = genre.getMovies().stream()
                    .map(movie -> movie.getTitle())
                    .collect(Collectors.toSet());
        } else {
            this.movieIds = Collections.emptySet();
            this.movieTitles = Collections.emptySet();
        }
    }
}
