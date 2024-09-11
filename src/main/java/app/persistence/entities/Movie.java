package app.persistence.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "movie")
@Data
@NoArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imdb_id", nullable = false)
    private Long imdbId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "overview", length = 1000)  // Øger længden til tegn, da 255 tegn er for lidt som er standard
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

    @ManyToMany
    @JoinTable(
            name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres;

    public Set<Integer> getGenreIds() {
        return genres.stream()
                .map(Genre::getGenreId)
                .collect(Collectors.toSet());
    }
}
