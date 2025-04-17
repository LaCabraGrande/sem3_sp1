package app.dtos;

import app.entities.Actor;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ActorDTO {
    private Long id;
    private String name;
    private Set<Long> movieIds;
    private Set<String> movieTitles;

    public ActorDTO(Actor actor) {
        this.id = actor.getId();
        this.name = actor.getName();

        if (Hibernate.isInitialized(actor.getMovies()) && actor.getMovies() != null) {
            this.movieIds = actor.getMovies().stream()
                    .map(movie -> movie.getId())
                    .collect(Collectors.toSet());

            this.movieTitles = actor.getMovies().stream()
                    .map(movie -> movie.getTitle())
                    .collect(Collectors.toSet());
        } else {
            this.movieIds = Collections.emptySet();
            this.movieTitles = Collections.emptySet();
        }
    }
}
