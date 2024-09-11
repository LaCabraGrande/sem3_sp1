package app.persistence.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "genre")
@Data
@NoArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Matcher Long id i GenreDTO

    @Column(name = "genre_id", unique = true, nullable = false)
    private int genreId;  // Matcher genreId i GenreDTO

    @Column(name = "name", nullable = false)
    private String name;  // Matcher name i GenreDTO

    @ManyToMany(mappedBy = "genres")
    private Set<Movie> movies;
}
