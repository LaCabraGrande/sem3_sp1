package app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "genre")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Matcher Long id i GenreDTO

    @Column(name = "genre_id", unique = true, nullable = false)
    private int genreId;  // Matcher genreId i GenreDTO

    @Column(name = "name", nullable = false)
    private String name;  // Matcher name i GenreDTO

    @ManyToMany(mappedBy = "genres")
    @JsonBackReference // Stopper serialisering her
    @ToString.Exclude
    private Set<Movie> movies;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return genreId == genre.genreId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(genreId);
    }
}
