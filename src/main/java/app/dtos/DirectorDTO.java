package app.dtos;

import app.entities.Director;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DirectorDTO {
    private Long id;
    private String name;
    private Set<Long> movieIds;
    private Set<String> movieTitles;

    public DirectorDTO(Director director) {
        this.id = director.getId();
        this.name = director.getName();

        if (Hibernate.isInitialized(director.getMovies()) && director.getMovies() != null) {
            this.movieIds = director.getMovies().stream()
                    .map(movie -> movie.getId())
                    .collect(Collectors.toSet());

            this.movieTitles = director.getMovies().stream()
                    .map(movie -> movie.getTitle())
                    .collect(Collectors.toSet());
        } else {
            this.movieIds = Collections.emptySet();
            this.movieTitles = Collections.emptySet();
        }
    }
}
