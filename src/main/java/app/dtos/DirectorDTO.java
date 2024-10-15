package app.dtos;

import app.entities.Director;
import lombok.*;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class DirectorDTO {
    private Long id;
    private String name;

    private Set<Long> movieIds;
    private Set<String> movieTitles;

    public DirectorDTO(Director director)
    {
        this.id = director.getId();
        this.name = director.getName();
        this.movieIds = director.getMovies().stream()
                .map(movie -> movie.getId())
                .collect(Collectors.toSet());
        this.movieTitles = director.getMovies().stream()
                .map(movie -> movie.getTitle())
                .collect(Collectors.toSet());
    }
}
