package app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "actor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    // Mange-til-mange relation til film
    @ManyToMany(mappedBy = "actors")
    @ToString.Exclude
    @JsonBackReference // Denne forhindrer uendelige loops ved at stoppe serialisering her
    private Set<Movie> movies;

    @Transient
    private Set<Long> movieIds;
    @Transient
    private Set<String> movieTitles;

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

