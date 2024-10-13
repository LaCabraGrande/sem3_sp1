package app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "genre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "genre_id", unique = true, nullable = false)
    private int genreId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany(mappedBy = "genres", fetch = FetchType.EAGER)
    @JsonBackReference
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
