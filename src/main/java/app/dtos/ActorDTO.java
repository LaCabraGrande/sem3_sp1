package app.dtos;

import app.entities.Actor;
import lombok.*;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ActorDTO {
    private Long id;
    private String name;

    private Set<Long> movieIds;
    private Set<String> movieTitles;

    public ActorDTO(Actor actor) {
        this.id = actor.getId();
        this.name = actor.getName();
        this.movieIds = actor.getMovies().stream()
                .map(movie -> movie.getId())
                .collect(Collectors.toSet());
        this.movieTitles = actor.getMovies().stream()
                .map(movie -> movie.getTitle())
                .collect(Collectors.toSet());
    }

}

