package app.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "actor")
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany(mappedBy = "actors", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore // Undgå problemer ved lazy-loading i JSON
    private Set<Movie> movies;

    @Transient
    private Set<Long> movieIds;

    @Transient
    private Set<String> movieTitles;

    // DTO-baseret constructor
    public Actor(app.dtos.ActorDTO dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.movieIds = dto.getMovieIds();
        this.movieTitles = dto.getMovieTitles();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Actor)) return false;
        Actor actor = (Actor) o;
        return Objects.equals(id, actor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
