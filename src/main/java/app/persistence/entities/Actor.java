package app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

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
}
