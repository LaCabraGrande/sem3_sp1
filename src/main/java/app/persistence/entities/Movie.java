package app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imdb_id", nullable = false)
    private Long imdbId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "overview", length = 1000)
    private String overview;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "adult")
    private boolean adult;

    @Column(name = "backdrop_path")
    private String backdropPath;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "popularity")
    private double popularity;

    @Column(name = "original_language")
    private String originalLanguage;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "vote_average")
    private double voteAverage;

    @Column(name = "vote_count")
    private int voteCount;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @ToString.Exclude
    @JsonManagedReference
    private Set<Genre> genres = new HashSet<>();

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "director_id", referencedColumnName = "id")
    @ToString.Exclude
    private Director director;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "movie_actor",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    @JsonManagedReference
    @ToString.Exclude
    private Set<Actor> actors = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(id, movie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
